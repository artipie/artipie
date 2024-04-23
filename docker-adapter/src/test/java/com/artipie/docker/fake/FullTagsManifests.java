/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.ImageTag;
import com.artipie.docker.misc.Pagination;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manifests implementation with specified tags.
 * Values of parameters `from` and `limit` from last call are captured.
 */
public final class FullTagsManifests implements Manifests {

    /**
     * Tags.
     */
    private final Tags tags;

    /**
     * From parameter captured.
     */
    private final AtomicReference<Pagination> from;

    public FullTagsManifests(final Tags tags) {
        this.tags = tags;
        this.from = new AtomicReference<>();
    }

    @Override
    public CompletableFuture<Manifest> put(final ManifestReference ref, final Content ignored) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Optional<Manifest>> get(final ManifestReference ref) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Tags> tags(Pagination pagination) {
        this.from.set(pagination);
        return CompletableFuture.completedFuture(this.tags);
    }

    /**
     * Get captured `from` argument.
     *
     * @return Captured `from` argument.
     */
    public Optional<String> capturedFrom() {
        return Optional.of(ImageTag.validate(this.from.get().last()));
    }

    /**
     * Get captured `limit` argument.
     *
     * @return Captured `limit` argument.
     */
    public int capturedLimit() {
        return from.get().limit();
    }
}
