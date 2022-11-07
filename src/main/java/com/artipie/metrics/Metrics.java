/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.artipie.metrics.publish.MetricsOutput;

/**
 * Registry of metrics by name.
 *
 * @since 0.6
 */
public interface Metrics {

    /**
     * Get counter metric by name.
     *
     * @param name Name of metric.
     * @return Counter metric instance.
     */
    Counter counter(String name);

    /**
     * Get gauge metric by name.
     *
     * @param name Name of metric.
     * @return Gauge metric instance.
     */
    Gauge gauge(String name);

    /**
     * Publish metrics to output.
     *
     * @param out Metrics output
     */
    void publish(MetricsOutput out);
}
