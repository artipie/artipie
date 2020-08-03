/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
            counters.put(entry.getKey(), entry.getValue().value());
        }
        out.gauges(gauges);
    }
}
