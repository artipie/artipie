/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.metrics.memory.InMemoryMetrics;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Periodic publisher of {@link InMemoryMetrics} to log.
 *
 * @since 0.9
 * @todo #231:30min Add tests for `MetricsLogPublisher`.
 *  It should be tested that the publisher runs periodically, collects fresh metrics data
 *  and logs the data as expected.
 */
public final class MetricsLogOutput implements IntervalMetricOutput {

    /**
     * Logger to use for publishing.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    private final Logger logger;

    /**
     * Counters map.
     */
    private final Map<String, Long> metrics;

    /**
     * Publishing interval.
     */
    private final Duration interval;

    /**
     * Ctor.
     *
     * @param logger Logger to use for publishing.
     * @param interval Publishing interval.
     */
    public MetricsLogOutput(final Logger logger, final Duration interval) {
        this.logger = logger;
        this.interval = interval;
        this.metrics = new ConcurrentHashMap<>();
    }

    @Override
    public Duration getInterval() {
        return this.interval;
    }

    @Override
    public void counters(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> entries : data.entrySet()) {
            final String key = entries.getKey();
            this.metrics.put(key, this.metrics.getOrDefault(key, 0L) + entries.getValue());
        }
        this.print();
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> entries : data.entrySet()) {
            final String key = entries.getKey();
            this.metrics.put(key, entries.getValue());
        }
        this.print();
    }

    /**
     * Print metrics to log.
     */
    private void print() {
        if (!this.metrics.isEmpty()) {
            final StringBuilder message = new StringBuilder("Counters:");
            for (final Map.Entry<String, Long> entry : this.metrics.entrySet()) {
                message.append('\n')
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue());
            }
            this.logger.info(message.toString());
        }
    }
}
