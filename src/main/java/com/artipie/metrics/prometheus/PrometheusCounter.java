/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import com.artipie.metrics.Counter;

/**
 * {@link Counter} implementation storing data in memory.
 *
 * @since 0.8
 */
public final class PrometheusCounter implements Counter {

    /**
     * Current counter value.
     */
    private final io.prometheus.client.Counter counter;

    /**
     * Current counter value.
     *
     * @param counter Is good
     */
    public PrometheusCounter(final io.prometheus.client.Counter counter) {
        this.counter = counter;
    }

    @Override
    public void add(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                String.format("Amount should not be negative: %d", amount)
            );
        }
        this.counter.inc(amount);
    }

    @Override
    public void inc() {
        this.counter.inc();
    }

    /**
     * Get counter value.
     *
     * @return Counter value.
     */
    public long value() {
        return (long) this.counter.get();
    }
}
