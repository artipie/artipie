/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * {@link Metrics} implementation storing data in memory.
 *
 * @since 0.9
 */
public final class InMemoryMetrics implements Metrics {

    /**
     * Counters by name.
     */
    private final ConcurrentMap<String, InMemoryCounter> counters = new ConcurrentHashMap<>();

    /**
     * Gauges by name.
     */
    private final ConcurrentMap<String, InMemoryGauge> gauges = new ConcurrentHashMap<>();

    @Override
    public InMemoryCounter counter(final String name) {
        return this.counters.computeIfAbsent(name, ignored -> new InMemoryCounter());
    }

    @Override
    public InMemoryGauge gauge(final String name) {
        return this.gauges.computeIfAbsent(name, ignored -> new InMemoryGauge());
    }

    @Override
    public void publish(final MetricsOutput out) {
        out.counters(InMemoryMetrics.metricToMap(this.counters, InMemoryCounter::value));
        out.gauges(InMemoryMetrics.metricToMap(this.gauges, InMemoryGauge::value));
    }

    /**
     * Get metrics data map by metrics map.
     *
     * @param source Metrics map.
     * @param transform Function to transform metric to long value.
     * @param <T> Metric type.
     * @return Metric data map.
     */
    private static <T> Map<String, Long> metricToMap(
        final Map<String, T> source,
        final Function<T, Long> transform) {
        final Map<String, Long> res = new HashMap<>(source.size());
        for (final Map.Entry<String, T> entry : source.entrySet()) {
            res.put(entry.getKey(), transform.apply(entry.getValue()));
        }
        return res;
    }
}
