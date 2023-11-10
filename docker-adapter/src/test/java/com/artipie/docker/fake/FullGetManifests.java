/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.JsonManifest;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Manifests implementation that contains manifest.
 *
 * @since 0.3
 */
public final class FullGetManifests implements Manifests {

    /**
     * Digest hex of manifest.
     */
    private final String hex;

    /**
     * Manifest content.
     */
    private final String content;

    /**
     * Ctor.
     *
     * @param hex Digest hex of manifest.
     */
    public FullGetManifests(final String hex) {
        this(hex, "");
    }

    /**
     * Ctor.
     *
     * @param hex Digest hex of manifest.
     * @param content Manifest content.
     */
    public FullGetManifests(final String hex, final String content) {
        this.hex = hex;
        this.content = content;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestRef ref, final Content ignored) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
        return CompletableFuture.completedFuture(
            Optional.of(
                new JsonManifest(
                    new Digest.Sha256(this.hex),
                    this.content.getBytes()
                )
            )
        );
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        throw new UnsupportedOperationException();
    }
}
