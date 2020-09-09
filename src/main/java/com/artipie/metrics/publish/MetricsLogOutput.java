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

import com.artipie.metrics.memory.InMemoryMetrics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Periodic publisher of {@link InMemoryMetrics} to log.
 *
 * @since 0.9
 * @todo #231:30min Add tests for `MetricsLogPublisher`.
 *  It should be tested that the publisher runs periodically, collects fresh metrics data
 *  and logs the data as expected.
 */
public final class MetricsLogOutput implements MetricsOutput {

    /**
     * Logger to use for publishing.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    private final Logger logger;

    /**
     * Counters map.
     */
    private final Map<String, Long> metrics;

    /**
     * Ctor.
     *
     * @param logger Logger to use for publishing.
     */
    public MetricsLogOutput(final Logger logger) {
        this.logger = logger;
        this.metrics = new ConcurrentHashMap<>();
    }

    @Override
    public void counters(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> entries : data.entrySet()) {
            final String key = entries.getKey();
            this.metrics.put(key, this.metrics.getOrDefault(key, 0L) + entries.getValue());
        }
        this.print();
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> entries : data.entrySet()) {
            final String key = entries.getKey();
            this.metrics.put(key, entries.getValue());
        }
        this.print();
    }

    /**
     * Print metrics to log.
     */
    private void print() {
        if (!this.metrics.isEmpty()) {
            final StringBuilder message = new StringBuilder("Counters:");
            for (final Map.Entry<String, Long> entry : this.metrics.entrySet()) {
                message.append('\n')
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue());
            }
            this.logger.info(message.toString());
        }
    }
}
