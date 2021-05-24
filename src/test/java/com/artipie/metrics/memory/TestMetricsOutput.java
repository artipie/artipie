/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.memory;

import com.artipie.metrics.publish.MetricsOutput;
import java.util.HashMap;
import java.util.Map;

/**
 * Test metrics output.
 * @since 0.19
 */
public final class TestMetricsOutput implements MetricsOutput {

    /**
     * Counters.
     */
    private final Map<String, Long> cnt;

    /**
     * Gauges.
     */
    private final Map<String, Long> ggs;

    /**
     * Ctor.
     */
    public TestMetricsOutput() {
        this.cnt = new HashMap<>();
        this.ggs = new HashMap<>();
    }

    @Override
    public void counters(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> entry : data.entrySet()) {
            final String key = entry.getKey();
            this.cnt.put(key, this.cnt.getOrDefault(key, 0L) + entry.getValue());
        }
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        this.ggs.putAll(data);
    }

    /**
     * Counters.
     * @return Map copy
     */
    public Map<String, Long> counters() {
        return new HashMap<>(this.cnt);
    }

    /**
     * Gauges.
     * @return Map copy
     */
    public Map<String, Long> gauges() {
        return new HashMap<>(this.ggs);
    }
}
