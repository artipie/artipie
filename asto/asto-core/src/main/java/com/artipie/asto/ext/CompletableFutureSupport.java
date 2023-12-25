/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.ext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Support of new {@link CompletableFuture} API for JDK 1.8.
 * @param <T> Future type
 * @since 0.33
 */
public abstract class CompletableFutureSupport<T> implements Supplier<CompletableFuture<T>> {

    /**
     * Supplier wrap.
     */
    private final Supplier<? extends CompletableFuture<T>> wrap;

    /**
     * New wrapped future supplier.
     * @param wrap Supplier to wrap
     */
    protected CompletableFutureSupport(final Supplier<? extends CompletableFuture<T>> wrap) {
        this.wrap = wrap;
    }

    @Override
    public final CompletableFuture<T> get() {
        return this.wrap.get();
    }

    /**
     * Failed completable future supplier.
     * @param <T> Future type
     * @since 0.33
     */
    public static final class Failed<T> extends CompletableFutureSupport<T> {
        /**
         * New failed future.
         * @param err Failure exception
         */
        public Failed(final Exception err) {
            super(() -> {
                final CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(err);
                return future;
            });
        }
    }

}
