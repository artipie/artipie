/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.asto.BlobSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Multi-read {@link Layers} implementation.
 *
 * @since 0.3
 */
public final class MultiReadLayers implements Layers {

    /**
     * Layers for reading.
     */
    private final List<Layers> layers;

    /**
     * Ctor.
     *
     * @param layers Layers for reading.
     */
    public MultiReadLayers(final List<Layers> layers) {
        this.layers = layers;
    }

    @Override
    public CompletionStage<Blob> put(final BlobSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Blob> mount(final Blob blob) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        final CompletableFuture<Optional<Blob>> promise = new CompletableFuture<>();
        CompletableFuture.allOf(
            this.layers.stream()
                .map(
                    layer -> layer.get(digest)
                        .thenAccept(
                            opt -> {
                                if (opt.isPresent()) {
                                    promise.complete(opt);
                                }
                            }
                        )
                        .toCompletableFuture()
                )
                .toArray(CompletableFuture[]::new)
        ).handle(
            (nothing, throwable) -> promise.complete(Optional.empty())
        );
        return promise;
    }
}
