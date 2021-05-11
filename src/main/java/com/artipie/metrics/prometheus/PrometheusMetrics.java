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
package com.artipie.metrics.prometheus;

import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link Metrics} implementation storing data in memory.
 *
 * @since 0.9
 */
public final class PrometheusMetrics implements Metrics {
    private static CollectorRegistry registry = new CollectorRegistry();
    private static final io.prometheus.client.Counter prometheusCounter = io.prometheus.client.Counter.build()
        .name("requests_total").help("Total requests.").register(registry);
    /**
     * Counters by name.
     */
    private final ConcurrentMap<String, PrometheusCounter> cnts = new ConcurrentHashMap<>();

    /**
     * Gauges by name.
     */
    private final ConcurrentMap<String, PrometheusGauge> ggs = new ConcurrentHashMap<>();

    @Override
    public PrometheusCounter counter(final String name) {
        return this.cnts.computeIfAbsent(name, ignored -> new PrometheusCounter());
    }

    @Override
    public PrometheusGauge gauge(final String name) {
        return this.ggs.computeIfAbsent(name, ignored -> new PrometheusGauge());
    }

    @Override
    public void publish(final MetricsOutput out) {
//        final Map<String, Long> counters = new HashMap<>(this.cnts.size());
//        for (final Map.Entry<String, PrometheusCounter> entry : this.cnts.entrySet()) {
//            counters.put(entry.getKey(), entry.getValue().value());
//        }
//        out.counters(counters);
//        final Map<String, Long> gauges = new HashMap<>(this.ggs.size());
//        for (final Map.Entry<String, PrometheusGauge> entry : this.ggs.entrySet()) {
//            counters.put(entry.getKey(), entry.getValue().value());
//        }
//        out.gauges(gauges);
        PushGateway pg = new PushGateway("prometheus.zhedge.xyz:9091");
        try {
            pg.pushAdd(registry, "my_batch_job");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
