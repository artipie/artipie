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

import com.jcabi.log.Logger;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Periodic publisher of {@link InMemoryMetrics} to log.
 *
 * @since 0.9
 * @todo #231:30min Support gauge publishing in `MetricsLogPublisher`.
 *  `InMemoryMetrics` contain gauges along counters.
 *  Gauges should be published the same way as counters.
 *  Should be done after https://github.com/artipie/artipie/issues/267
 */
public class MetricsLogPublisher {

    /**
     * Metrics for publishing.
     */
    private final InMemoryMetrics metrics;

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
    public MetricsLogPublisher(final InMemoryMetrics metrics, final Duration period) {
        this.metrics = metrics;
        this.period = period;
    }

    /**
     * Start periodic publishing.
     */
    public void start() {
        final long millis = this.period.toMillis();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
            this::publish,
            millis,
            millis,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Publish metrics to log.
     */
    private void publish() {
        final Map<String, InMemoryCounter> counters = new TreeMap<>(this.metrics.counters());
        final StringBuilder message = new StringBuilder("Counters:");
        for (final Map.Entry<String, InMemoryCounter> entry : counters.entrySet()) {
            message.append('\n')
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue().value());
        }
        Logger.info(this.metrics, message.toString());
    }
}
