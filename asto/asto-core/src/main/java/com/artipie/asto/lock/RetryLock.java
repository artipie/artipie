/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.internal.InMemoryRetryRegistry;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Lock that tries to obtain origin {@link Lock} with retries.
 *
 * @since 0.24
 */
public final class RetryLock implements Lock {

    /**
     * Max number of attempts by default.
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Scheduler to use for retry triggering.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Origin lock.
     */
    private final Lock origin;

    /**
     * Retry registry to store retries state.
     */
    private final InMemoryRetryRegistry registry;

    /**
     * Ctor.
     *
     * @param scheduler Scheduler to use for retry triggering.
     * @param origin Origin lock.
     */
    public RetryLock(final ScheduledExecutorService scheduler, final Lock origin) {
        this(
            scheduler,
            origin,
            new RetryConfig.Builder<>()
                .maxAttempts(RetryLock.MAX_ATTEMPTS)
                .intervalFunction(IntervalFunction.ofExponentialBackoff())
                .build()
        );
    }

    /**
     * Ctor.
     *
     * @param scheduler Scheduler to use for retry triggering.
     * @param origin Origin lock.
     * @param config Retry strategy.
     */
    public RetryLock(
        final ScheduledExecutorService scheduler,
        final Lock origin,
        final RetryConfig config
    ) {
        this.scheduler = scheduler;
        this.origin = origin;
        this.registry = new InMemoryRetryRegistry(config);
    }

    @Override
    public CompletionStage<Void> acquire() {
        return this.registry.retry("lock-acquire").executeCompletionStage(
            this.scheduler,
            this.origin::acquire
        );
    }

    @Override
    public CompletionStage<Void> release() {
        return this.registry.retry("lock-release").executeCompletionStage(
            this.scheduler,
            this.origin::release
        );
    }
}
