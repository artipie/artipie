/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.asto.CheckedBlobSource;
import com.artipie.docker.manifest.Layer;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.JoinedTagsSource;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.scheduling.ArtifactEvent;
import com.jcabi.log.Logger;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Cache implementation of {@link Repo}.
 *
 * @since 0.3
 */
public final class CacheManifests implements Manifests {

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "docker-proxy";

    /**
     * Repository (image) name.
     */
    private final RepoName name;

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
     * Ctor.
     *
     * @param name Repository name.
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact metadata events
     * @param rname Artipie repository name
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public CacheManifests(final RepoName name, final Repo origin, final Repo cache,
        final Optional<Queue<ArtifactEvent>> events, final String rname) {
        this.name = name;
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
        return this.origin.manifests().get(ref).handle(
            (original, throwable) -> {
                final CompletionStage<Optional<Manifest>> result;
                if (throwable == null) {
                    if (original.isPresent()) {
                        this.copy(ref);
                        result = CompletableFuture.completedFuture(original);
                    } else {
                        result = this.cache.manifests().get(ref).exceptionally(ignored -> original);
                    }
                } else {
                    result = this.cache.manifests().get(ref);
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        return new JoinedTagsSource(
            this.name, from, limit, this.origin.manifests(), this.cache.manifests()
        ).tags();
    }

    /**
     * Copy manifest by reference from original to cache.
     *
     * @param ref Manifest reference.
     * @return Copy completion.
     */
    private CompletionStage<Void> copy(final ManifestRef ref) {
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
                                CacheManifests.REPO_TYPE, this.rname,
                                ArtifactEvent.DEF_OWNER, this.name.value(), ref.string(),
                                manifest.layers().stream().mapToLong(Layer::size).sum()
                            )
                        )
                    );
                    return res;
                }
            )
        ).handle(
            (ignored, ex) -> {
                if (ex != null) {
                    Logger.error(
                        this, "Failed to cache manifest %s: %[exception]s", ref.string(), ex
                    );
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
                if (!blob.isPresent()) {
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
