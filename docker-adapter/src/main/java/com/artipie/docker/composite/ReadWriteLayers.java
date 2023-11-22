/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.asto.BlobSource;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Read-write {@link Layers} implementation.
 *
 * @since 0.3
 */
public final class ReadWriteLayers implements Layers {

    /**
     * Layers for reading.
     */
    private final Layers read;

    /**
     * Layers for writing.
     */
    private final Layers write;

    /**
     * Ctor.
     *
     * @param read Layers for reading.
     * @param write Layers for writing.
     */
    public ReadWriteLayers(final Layers read, final Layers write) {
        this.read = read;
        this.write = write;
    }

    @Override
    public CompletionStage<Blob> put(final BlobSource source) {
        return this.write.put(source);
    }

    @Override
    public CompletionStage<Blob> mount(final Blob blob) {
        return this.write.mount(blob);
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        return this.read.get(digest);
    }
}
