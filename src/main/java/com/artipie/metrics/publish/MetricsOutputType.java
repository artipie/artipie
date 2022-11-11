/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

/**
 * Metrics output types.
 *
 * @since 0.28.0
 */
public enum MetricsOutputType {
    /**
     * Storage metrics output.
     */
    ASTO(true),

    /**
     * Log metrics output.
     */
    LOG(true),

    /**
     * Vert.x metrics.
     */
    VERTX(false),

    /**
     * Prometheus's metrics output.
     */
    PROMETHEUS(false);

    /**
     * Metrics output with publishing interval.
     */
    private final boolean interval;

    /**
     * Ctor.
     *
     * @param interval Publishing interval flag.
     */
    MetricsOutputType(final boolean interval) {
        this.interval = interval;
    }

    /**
     * Metrics output type with a publishing interval.
     *
     * @return True
     */
    public boolean isInterval() {
        return this.interval;
    }
}
