/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Digest;
import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.error.InvalidManifestException;
import com.artipie.docker.manifest.JsonManifest;
import com.artipie.docker.manifest.Layer;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.json.JsonException;

/**
 * Asto implementation of {@link Manifests}.
 *
 * @since 0.3
 */
public final class AstoManifests implements Manifests {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Blobs storage.
     */
    private final BlobStore blobs;

    /**
     * Manifests layout.
     */
    private final ManifestsLayout layout;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param asto Asto storage
     * @param blobs Blobs storage.
     * @param layout Manifests layout.
     * @param name Repository name
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public AstoManifests(
        final Storage asto,
        final BlobStore blobs,
        final ManifestsLayout layout,
        final RepoName name
    ) {
        this.asto = asto;
        this.blobs = blobs;
        this.layout = layout;
        this.name = name;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
        return new PublisherAs(content).bytes().thenCompose(
            bytes -> this.blobs.put(new TrustedBlobSource(bytes))
                .thenApply(blob -> new JsonManifest(blob.digest(), bytes))
                .thenCompose(
                    manifest -> this.validate(manifest)
                        .thenCompose(nothing -> this.addManifestLinks(ref, manifest.digest()))
                        .thenApply(nothing -> manifest)
                )
        );
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
        return this.readLink(ref).thenCompose(
            digestOpt -> digestOpt.map(
                digest -> this.blobs.blob(digest)
                    .thenCompose(
                        blobOpt -> blobOpt
                            .map(
                                blob -> blob.content()
                                    .thenApply(PublisherAs::new)
                                    .thenCompose(PublisherAs::bytes)
                                    .<Manifest>thenApply(
                                        bytes -> new JsonManifest(blob.digest(), bytes)
                                    )
                                    .thenApply(Optional::of)
                            )
                            .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))
                    )
            ).orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()))
        );
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        final Key root = this.layout.tags(this.name);
        return this.asto.list(root).thenApply(
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
                    digest -> this.blobs.blob(digest).thenCompose(
                        opt -> {
                            if (!opt.isPresent()) {
                                throw new InvalidManifestException(
                                    String.format("Blob does not exist: %s", digest)
                                );
                            }
                            return CompletableFuture.allOf();
                        }
                    ).toCompletableFuture()
                ),
                Stream.of(
                    CompletableFuture.runAsync(
                        () -> {
                            if (manifest.mediaTypes().isEmpty()) {
                                throw new InvalidManifestException(
                                    "Required field `mediaType` is empty"
                                );
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
    private CompletableFuture<Void> addManifestLinks(final ManifestRef ref, final Digest digest) {
        return CompletableFuture.allOf(
            this.addLink(new ManifestRef.FromDigest(digest), digest),
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
    private CompletableFuture<Void> addLink(final ManifestRef ref, final Digest digest) {
        return this.asto.save(
            this.layout.manifest(this.name, ref),
            new Content.From(digest.string().getBytes(StandardCharsets.US_ASCII))
        ).toCompletableFuture();
    }

    /**
     * Reads link to blob by manifest reference.
     *
     * @param ref Manifest reference.
     * @return Blob digest, empty if no link found.
     */
    private CompletableFuture<Optional<Digest>> readLink(final ManifestRef ref) {
        final Key key = this.layout.manifest(this.name, ref);
        return this.asto.exists(key).thenCompose(
            exists -> {
                final CompletionStage<Optional<Digest>> stage;
                if (exists) {
                    stage = this.asto.value(key)
                        .thenCompose(
                            pub -> new PublisherAs(pub).asciiString()
                        )
                        .<Digest>thenApply(Digest.FromString::new)
                        .thenApply(Optional::of);
                } else {
                    stage = CompletableFuture.completedFuture(Optional.empty());
                }
                return stage;
            }
        );
    }
}
