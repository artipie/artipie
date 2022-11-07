/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.nop;

import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;

/**
 * {@link Metrics} implementation that do no operations and store no data.
 *
 * @since 0.9
 */
public final class NopMetrics implements Metrics {

    /**
     * Only instance of {@link NopMetrics}.
     */
    public static final NopMetrics INSTANCE = new NopMetrics();

    /**
     * Ctor.
     */
    private NopMetrics() {
    }

    @Override
    public NopCounter counter(final String name) {
        return NopCounter.INSTANCE;
    }

    @Override
    public NopGauge gauge(final String name) {
        return NopGauge.INSTANCE;
    }

    @Override
    public void publish(final MetricsOutput out) {
        throw new IllegalStateException("NopMetrics should not be used to publish metrics");
    }
}
