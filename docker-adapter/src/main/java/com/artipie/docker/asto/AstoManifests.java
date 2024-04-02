/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Digest;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.error.InvalidManifestException;
import com.artipie.docker.manifest.JsonManifest;
import com.artipie.docker.manifest.Layer;
import com.artipie.docker.manifest.Manifest;
import com.google.common.base.Strings;

import javax.json.JsonException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * Asto implementation of {@link Manifests}.
 */
public final class AstoManifests implements Manifests {

    /**
     * Asto storage.
     */
    private final Storage storage;

    /**
     * Blobs storage.
     */
    private final Blobs blobs;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * @param asto Asto storage
     * @param blobs Blobs storage.
     * @param name Repository name
     */
    public AstoManifests(Storage asto, Blobs blobs, RepoName name) {
        this.storage = asto;
        this.blobs = blobs;
        this.name = name;
    }

    @Override
    public CompletableFuture<Manifest> put(ManifestReference ref, Content content) {
        return content.asBytesFuture()
            .thenCompose(bytes -> this.blobs.put(new TrustedBlobSource(bytes))
                .thenApply(blob -> new JsonManifest(blob.digest(), bytes))
                .thenCompose(
                    manifest -> this.validate(manifest)
                        .thenCompose(nothing -> this.addManifestLinks(ref, manifest.digest()))
                        .thenApply(nothing -> manifest)
                )
            );
    }

    @Override
    public CompletableFuture<Optional<Manifest>> get(final ManifestReference ref) {
        return this.readLink(ref).thenCompose(
            digestOpt -> digestOpt.map(
                digest -> this.blobs.blob(digest)
                    .thenCompose(
                        blobOpt -> blobOpt
                            .map(
                                blob -> blob.content()
                                    .thenCompose(Content::asBytesFuture)
                                    .thenApply(bytes -> Optional.<Manifest>of(
                                        new JsonManifest(blob.digest(), bytes))
                                    )
                            )
                            .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))
                    )
            ).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))
        );
    }

    @Override
    public CompletableFuture<Tags> tags(final Optional<Tag> from, final int limit) {
        final Key root = Layout.tags(this.name);
        return this.storage.list(root).thenApply(
            keys -> new AstoTags(this.name, root, keys, from, limit)
        );
    }

    /**
     * Validates manifest by checking all referenced blobs exist.
     *
     * @param manifest Manifest.
     * @return Validation completion.
     */
    private CompletionStage<Void> validate(final Manifest manifest) {
        final Stream<Digest> digests;
        try {
            digests = Stream.concat(
                Stream.of(manifest.config()),
                manifest.layers().stream()
                    .filter(layer -> layer.urls().isEmpty())
                    .map(Layer::digest)
            );
        } catch (final JsonException ex) {
            throw new InvalidManifestException(
                String.format("Failed to parse manifest: %s", ex.getMessage()),
                ex
            );
        }
        return CompletableFuture.allOf(
            Stream.concat(
                digests.map(
                    digest -> this.blobs.blob(digest)
                        .thenCompose(
                            opt -> {
                                if (opt.isEmpty()) {
                                    throw new InvalidManifestException("Blob does not exist: " + digest);
                                }
                                return CompletableFuture.allOf();
                            }
                        ).toCompletableFuture()
                ),
                Stream.of(
                    CompletableFuture.runAsync(
                        () -> {
                            if(Strings.isNullOrEmpty(manifest.mediaType())){
                                throw new InvalidManifestException("Required field `mediaType` is empty");
                            }
                        }
                    )
                )
            ).toArray(CompletableFuture[]::new)
        );
    }

    /**
     * Adds links to manifest blob by reference and by digest.
     *
     * @param ref Manifest reference.
     * @param digest Blob digest.
     * @return Signal that links are added.
     */
    private CompletableFuture<Void> addManifestLinks(final ManifestReference ref, final Digest digest) {
        return CompletableFuture.allOf(
            this.addLink(ManifestReference.from(digest), digest),
            this.addLink(ref, digest)
        );
    }

    /**
     * Puts link to blob to manifest reference path.
     *
     * @param ref Manifest reference.
     * @param digest Blob digest.
     * @return Link key.
     */
    private CompletableFuture<Void> addLink(final ManifestReference ref, final Digest digest) {
        return this.storage.save(
            Layout.manifest(this.name, ref),
            new Content.From(digest.string().getBytes(StandardCharsets.US_ASCII))
        ).toCompletableFuture();
    }

    /**
     * Reads link to blob by manifest reference.
     *
     * @param ref Manifest reference.
     * @return Blob digest, empty if no link found.
     */
    private CompletableFuture<Optional<Digest>> readLink(final ManifestReference ref) {
        final Key key = Layout.manifest(this.name, ref);
        return this.storage.exists(key).thenCompose(
            exists -> {
                if (exists) {
                    return this.storage.value(key)
                        .thenCompose(Content::asStringFuture)
                        .thenApply(val -> Optional.of(new Digest.FromString(val)));
                }
                return CompletableFuture.completedFuture(Optional.empty());
            }
        );
    }
}
