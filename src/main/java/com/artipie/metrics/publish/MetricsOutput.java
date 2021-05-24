/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import java.util.Map;

/**
 * Metrics output to accepts published data.
 * @since 0.19
 */
public interface MetricsOutput {

    /**
     * Accepts counters data to increment existing data.
     * @param data Counters by name
     */
    void counters(Map<String, Long> data);

    /**
     * Accepts gauge data to change existing data.
     * @param data Gauges by name
     */
    void gauges(Map<String, Long> data);
}
