/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Key;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Cache control.
 * @since 0.24
 */
public interface CacheControl {

    /**
     * Validate cached item: checks if cached value can be used or needs to be updated by fresh
     * value.
     * @param item Cached item
     * @param content Content supplier
     * @return True if cached item can be used, false if needs to be updated
     */
    CompletionStage<Boolean> validate(Key item, Remote content);

    /**
     * Standard cache controls.
     * @since 0.24
     */
    enum Standard implements CacheControl {
        /**
         * Don't use cache, always invalidate.
         */
        NO_CACHE((item, content) -> CompletableFuture.completedFuture(false)),
        /**
         * Always use cache, don't invalidate.
         */
        ALWAYS((item, content) -> CompletableFuture.completedFuture(true));

        /**
         * Origin cache control.
         */
        private final CacheControl origin;

        /**
         * Ctor.
         * @param origin Cache control
         */
        Standard(final CacheControl origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Boolean> validate(final Key item, final Remote supplier) {
            return this.origin.validate(item, supplier);
        }
    }

    /**
     * All cache controls should validate the cache.
     * @since 0.25
     */
    final class All implements CacheControl {

        /**
         * Cache control items.
         */
        private final Collection<CacheControl> items;

        /**
         * All of items should validate the cache.
         * @param items Cache controls
         */
        public All(final CacheControl... items) {
            this(Arrays.asList(items));
        }

        /**
         * All of items should validate the cache.
         * @param items Cache controls
         */
        public All(final Collection<CacheControl> items) {
            this.items = items;
        }

        @Override
        public CompletionStage<Boolean> validate(final Key key, final Remote content) {
            return Observable.fromIterable(this.items)
                .flatMapSingle(item -> SingleInterop.fromFuture(item.validate(key, content)))
                .all(item -> item)
                .to(SingleInterop.get());
        }
    }
}
