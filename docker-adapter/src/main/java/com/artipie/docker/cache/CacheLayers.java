/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.asto.BlobSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Cache implementation of {@link Layers}.
 *
 * @since 0.3
 */
public final class CacheLayers implements Layers {

    /**
     * Origin layers.
     */
    private final Layers origin;

    /**
     * Cache layers.
     */
    private final Layers cache;

    /**
     * Ctor.
     *
     * @param origin Origin layers.
     * @param cache Cache layers.
     */
    public CacheLayers(final Layers origin, final Layers cache) {
        this.origin = origin;
        this.cache = cache;
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
        return this.cache.get(digest).handle(
            (cached, throwable) -> {
                final CompletionStage<Optional<Blob>> result;
                if (throwable == null) {
                    if (cached.isPresent()) {
                        result = CompletableFuture.completedFuture(cached);
                    } else {
                        result = this.origin.get(digest).exceptionally(ignored -> cached);
                    }
                } else {
                    result = this.origin.get(digest);
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }
}
