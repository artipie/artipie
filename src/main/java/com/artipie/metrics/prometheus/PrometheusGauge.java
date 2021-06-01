/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import com.artipie.metrics.Gauge;

/**
 * {@link Gauge} implementation storing data in memory.
 *
 * @since 0.8
 */
public final class PrometheusGauge implements Gauge {

    /**
     * Current value.
     */
    private final io.prometheus.client.Gauge current;

    /**
     * Current counter value.
     *
     * @param gauge Is good
     */
    public PrometheusGauge(final io.prometheus.client.Gauge gauge) {
        this.current = gauge;
    }

    @Override
    public void set(final long update) {
        this.current.set(update);
    }

    /**
     * Get gauge value.
     *
     * @return Gauge value.
     */
    public long value() {
        return (long) this.current.get();
    }
}
