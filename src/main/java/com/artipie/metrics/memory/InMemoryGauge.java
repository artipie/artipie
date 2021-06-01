/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import com.artipie.metrics.Gauge;

/**
 * {@link Gauge} implementation storing data in memory.
 *
 * @since 0.8
 */
public final class InMemoryGauge implements Gauge {

    /**
     * Current value.
     */
    private volatile long current;

    @Override
    public void set(final long update) {
        this.current = update;
    }

    /**
     * Get gauge value.
     *
     * @return Gauge value.
     */
    public long value() {
        return this.current;
    }
}
