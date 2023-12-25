/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock;

import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;

/**
 * Reactive adapter for {@link Lock}.
 *
 * @since 0.27
 */
public final class RxLock {

    /**
     * Origin.
     */
    private final Lock origin;

    /**
     * Ctor.
     *
     * @param origin Origin.
     */
    public RxLock(final Lock origin) {
        this.origin = origin;
    }

    /**
     * Acquire the lock.
     *
     * @return Completion of lock acquire operation.
     */
    public Completable acquire() {
        return Completable.defer(() -> CompletableInterop.fromFuture(this.origin.acquire()));
    }

    /**
     * Release the lock.
     *
     * @return Completion of lock release operation.
     */
    public Completable release() {
        return Completable.defer(() -> CompletableInterop.fromFuture(this.origin.release()));
    }
}
