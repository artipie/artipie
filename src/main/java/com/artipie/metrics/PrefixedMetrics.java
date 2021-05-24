/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.artipie.metrics.publish.MetricsOutput;

/**
 * Metrics that adds prefix to names.
 *
 * @since 0.9
 */
public final class PrefixedMetrics implements Metrics {

    /**
     * Origin metrics.
     */
    private final Metrics origin;

    /**
     * Prefix.
     */
    private final String prefix;

    /**
     * Ctor.
     *
     * @param origin Origin metrics.
     * @param prefix Prefix.
     */
    public PrefixedMetrics(final Metrics origin, final String prefix) {
        this.origin = origin;
        this.prefix = prefix;
    }

    @Override
    public Counter counter(final String name) {
        return this.origin.counter(this.prefix + name);
    }

    @Override
    public Gauge gauge(final String name) {
        return this.origin.gauge(this.prefix + name);
    }

    @Override
    public void publish(final MetricsOutput out) {
        this.origin.publish(out);
    }
}
