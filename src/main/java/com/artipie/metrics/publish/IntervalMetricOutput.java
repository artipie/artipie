/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import java.time.Duration;

/**
 * Metrics output with a publishing interval.
 * @since 0.28.0
 */
public interface IntervalMetricOutput extends MetricsOutput {
    /**
     * Publishing interval.
     *
     * @return Duration.
     */
    Duration getInterval();
}
