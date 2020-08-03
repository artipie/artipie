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
