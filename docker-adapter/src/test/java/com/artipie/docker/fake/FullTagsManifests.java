/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manifests implementation with specified tags.
 * Values of parameters `from` and `limit` from last call are captured.
 */
public final class FullTagsManifests implements Manifests {

    /**
     * Tags.
     */
    private final Tags tgs;

    /**
     * From parameter captured.
     */
    private final AtomicReference<Optional<Tag>> from;

    /**
     * Limit parameter captured.
     */
    private final AtomicInteger limit;

    /**
     * Ctor.
     *
     * @param tgs Tags.
     */
    public FullTagsManifests(final Tags tgs) {
        this.tgs = tgs;
        this.from = new AtomicReference<>();
        this.limit = new AtomicInteger();
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
    public CompletableFuture<Tags> tags(final Optional<Tag> pfrom, final int plimit) {
        this.from.set(pfrom);
        this.limit.set(plimit);
        return CompletableFuture.completedFuture(this.tgs);
    }

    /**
     * Get captured `from` argument.
     *
     * @return Captured `from` argument.
     */
    public Optional<Tag> capturedFrom() {
        return this.from.get();
    }

    /**
     * Get captured `limit` argument.
     *
     * @return Captured `limit` argument.
     */
    public int capturedLimit() {
        return this.limit.get();
    }
}
