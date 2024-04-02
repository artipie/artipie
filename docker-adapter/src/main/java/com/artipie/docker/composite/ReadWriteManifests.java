/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Read-write {@link Manifests} implementation.
 */
public final class ReadWriteManifests implements Manifests {

    /**
     * Manifests for reading.
     */
    private final Manifests read;

    /**
     * Manifests for writing.
     */
    private final Manifests write;

    /**
     * Ctor.
     *
     * @param read Manifests for reading.
     * @param write Manifests for writing.
     */
    public ReadWriteManifests(final Manifests read, final Manifests write) {
        this.read = read;
        this.write = write;
    }

    @Override
    public CompletableFuture<Manifest> put(final ManifestReference ref, final Content content) {
        return this.write.put(ref, content);
    }

    @Override
    public CompletableFuture<Optional<Manifest>> get(final ManifestReference ref) {
        return this.read.get(ref);
    }

    @Override
    public CompletableFuture<Tags> tags(final Optional<Tag> from, final int limit) {
        return this.read.tags(from, limit);
    }
}
