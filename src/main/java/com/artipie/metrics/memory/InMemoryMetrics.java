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

/**
 * {@link Metrics} implementation storing data in memory.
 *
 * @since 0.9
 */
public final class InMemoryMetrics implements Metrics {

    /**
     * Counters by name.
     */
    private final ConcurrentMap<String, InMemoryCounter> cnts = new ConcurrentHashMap<>();

    /**
     * Gauges by name.
     */
    private final ConcurrentMap<String, InMemoryGauge> ggs = new ConcurrentHashMap<>();

    @Override
    public InMemoryCounter counter(final String name) {
        return this.cnts.computeIfAbsent(name, ignored -> new InMemoryCounter());
    }

    @Override
    public InMemoryGauge gauge(final String name) {
        return this.ggs.computeIfAbsent(name, ignored -> new InMemoryGauge());
    }

    @Override
    public void publish(final MetricsOutput out) {
        final Map<String, Long> counters = new HashMap<>(this.cnts.size());
        for (final Map.Entry<String, InMemoryCounter> entry : this.cnts.entrySet()) {
            counters.put(entry.getKey(), entry.getValue().value());
        }
        out.counters(counters);
        final Map<String, Long> gauges = new HashMap<>(this.ggs.size());
        for (final Map.Entry<String, InMemoryGauge> entry : this.ggs.entrySet()) {
            gauges.put(entry.getKey(), entry.getValue().value());
        }
        out.gauges(gauges);
    }
}
