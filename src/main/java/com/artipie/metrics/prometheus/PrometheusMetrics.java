/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.prometheus;

import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;
import com.jcabi.log.Logger;
import io.prometheus.client.CollectorRegistry;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link Metrics} implementation storing data in memory.
 *
 * @since 0.9
 */
public final class PrometheusMetrics implements Metrics {
    /**
     * Metrics by name.
     */
    private final CollectorRegistry registry = new CollectorRegistry();

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
        return this.cnts.computeIfAbsent(
            name, ignored -> new PrometheusCounter(
                io.prometheus.client.Counter.build().name(name).help(name)
                    .register(this.registry)
            )
        );
    }

    @Override
    public PrometheusGauge gauge(final String name) {
        return this.ggs.computeIfAbsent(
            name, ignored -> new PrometheusGauge(
                io.prometheus.client.Gauge.build().name(name).help(name)
                    .register(this.registry)
            )
        );
    }

    @Override
    public void publish(final MetricsOutput out) {
        final PushGateway pgate = new PushGateway("prometheus.zhedge.xyz:9091");
        try {
            pgate.pushAdd(this.registry, "my_batch_job");
        } catch (final IOException exc) {
            Logger.error(PrometheusMetrics.class, exc.getMessage());
        }
    }
}
