/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.Tags;
import com.artipie.docker.asto.CheckedBlobSource;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.manifest.ManifestLayer;
import com.artipie.docker.misc.JoinedTagsSource;
import com.artipie.docker.misc.Pagination;
import com.artipie.scheduling.ArtifactEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Cache implementation of {@link Repo}.
 */
public final class CacheManifests implements Manifests {

    private final static Logger LOGGER = LoggerFactory.getLogger(CacheManifests.class);

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "docker-proxy";

    /**
     * Repository (image) name.
     */
    private final String name;

    /**
     * Origin repository.
     */
    private final Repo origin;

    /**
     * Cache repository.
     */
    private final Repo cache;

    /**
     * Events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Artipie repository name.
     */
    private final String rname;

    /**
     * @param name Repository name.
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact metadata events
     * @param registryName Artipie repository name
     */
    public CacheManifests(String name, Repo origin, Repo cache,
        Optional<Queue<ArtifactEvent>> events, String registryName) {
        this.name = name;
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.rname = registryName;
    }

    @Override
    public CompletableFuture<Manifest> put(final ManifestReference ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Optional<Manifest>> get(final ManifestReference ref) {
        return this.origin.manifests().get(ref).handle(
            (original, throwable) -> {
                final CompletionStage<Optional<Manifest>> result;
                if (throwable == null) {
                    if (original.isPresent()) {
                        Manifest manifest = original.get();
                        if (Manifest.MANIFEST_SCHEMA2.equals(manifest.mediaType()) ||
                            Manifest.MANIFEST_OCI_V1.equals(manifest.mediaType())) {
                            result = this.copy(ref).thenApply(unused -> original);
                        } else {
                            LOGGER.warn("Cannot add manifest to cache: [manifest={}, mediaType={}]",
                                    ref.digest(), manifest.mediaType());
                            result = CompletableFuture.completedFuture(original);
                        }
                    } else {
                        result = this.cache.manifests().get(ref).exceptionally(ignored -> original);
                    }
                } else {
                    LOGGER.error("Failed getting manifest ref=" + ref.digest(), throwable);
                    result = this.cache.manifests().get(ref);
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Tags> tags(Pagination pagination) {
        return new JoinedTagsSource(
            this.name, pagination, this.origin.manifests(), this.cache.manifests()
        ).tags();
    }

    /**
     * Copy manifest by reference from original to cache.
     *
     * @param ref Manifest reference.
     * @return Copy completion.
     */
    private CompletionStage<Void> copy(final ManifestReference ref) {
        return this.origin.manifests().get(ref).thenApply(Optional::get).thenCompose(
            manifest -> CompletableFuture.allOf(
                this.copy(manifest.config()).toCompletableFuture(),
                CompletableFuture.allOf(
                    manifest.layers().stream()
                        .filter(layer -> layer.urls().isEmpty())
                        .map(layer -> this.copy(layer.digest()).toCompletableFuture())
                        .toArray(CompletableFuture[]::new)
                ).toCompletableFuture()
            ).thenCompose(
                nothing -> {
                    final CompletionStage<Manifest> res =
                        this.cache.manifests().put(ref, manifest.content());
                    this.events.ifPresent(
                        queue -> queue.add(
                            new ArtifactEvent(
                                CacheManifests.REPO_TYPE,
                                this.rname,
                                ArtifactEvent.DEF_OWNER,
                                this.name,
                                ref.digest(),
                                manifest.layers().stream().mapToLong(ManifestLayer::size).sum()
                            )
                        )
                    );
                    return res;
                }
            )
        ).handle(
            (ignored, ex) -> {
                if (ex != null) {
                    LOGGER.error("Failed to cache manifest " + ref.digest(), ex);
                }
                return null;
            }
        );
    }

    /**
     * Copy blob by digest from original to cache.
     *
     * @param digest Blob digest.
     * @return Copy completion.
     */
    private CompletionStage<Void> copy(final Digest digest) {
        return this.origin.layers().get(digest).thenCompose(
            blob -> {
                if (blob.isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Failed loading blob %s", digest)
                    );
                }
                return blob.get().content();
            }
        ).thenCompose(
            content -> this.cache.layers().put(new CheckedBlobSource(content, digest))
        ).thenCompose(
            blob -> CompletableFuture.allOf()
        );
    }
}
