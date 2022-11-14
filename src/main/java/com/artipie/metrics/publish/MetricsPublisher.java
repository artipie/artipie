/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.metrics.Metrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Metrics periodical publisher.
 *
 * @since 0.19
 */
public final class MetricsPublisher {

    /**
     * Metrics for publishing.
     */
    private final Metrics metrics;

    /**
     * List of metrics output executors.
     */
    private final List<ExecutorService> executors;

    /**
     * Collection of metrics output to accept published data.
     */
    private final Collection<IntervalMetricOutput> outputs;

    /**
     * Ctor.
     *
     * @param metrics Metrics for publishing.
     * @param outputs Outputs.
     */
    public MetricsPublisher(final Metrics metrics, final Collection<IntervalMetricOutput> outputs) {
        this.metrics = metrics;
        this.outputs = outputs;
        this.executors = new ArrayList<>(outputs.size());
    }

    /**
     * Start periodic publishing.
     */
    public void start() {
        for (final IntervalMetricOutput out : this.outputs) {
            final long millis = out.getInterval().toMillis();
            final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            this.executors.add(executor);
            executor.scheduleAtFixedRate(
                () -> this.metrics.publish(out),
                millis, millis, TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Shutdowns metrics output executors.
     */
    public void stop() {
        this.executors.forEach(ExecutorService::shutdown);
    }
}
