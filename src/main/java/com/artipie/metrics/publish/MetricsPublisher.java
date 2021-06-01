/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.metrics.Metrics;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Metrics periodical publisher.
 * @since 0.19
 */
public final class MetricsPublisher {

    /**
     * Metrics for publishing.
     */
    private final Metrics metrics;

    /**
     * Period.
     */
    private final Duration period;

    /**
     * Ctor.
     *
     * @param metrics Metrics for publishing.
     * @param period Period.
     */
    public MetricsPublisher(final Metrics metrics, final Duration period) {
        this.metrics = metrics;
        this.period = period;
    }

    /**
     * Start periodic publishing.
     * @param output Output for publishing
     */
    public void start(final MetricsOutput output) {
        final long millis = this.period.toMillis();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            () -> this.metrics.publish(output),
            millis, millis, TimeUnit.MILLISECONDS
        );
    }
}
