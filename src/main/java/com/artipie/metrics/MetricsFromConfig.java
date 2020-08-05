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
package com.artipie.metrics;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.YamlStorage;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.metrics.memory.InMemoryMetrics;
import com.artipie.metrics.publish.MetricsLogOutput;
import com.artipie.metrics.publish.MetricsOutput;
import com.artipie.metrics.publish.MetricsPublisher;
import com.artipie.metrics.publish.StorageMetricsOutput;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.LoggerFactory;

/**
 * Metrics from config.
 * @since 0.9
 */
public final class MetricsFromConfig {

    /**
     * Metrics section from settings.
     */
    private final YamlMapping settings;

    /**
     * Ctor.
     * @param metrics Yaml settings
     */
    public MetricsFromConfig(final YamlMapping metrics) {
        this.settings = metrics;
    }

    /**
     * Returns {@link Metrics} instance according to configuration.
     * @return Instance of {@link Metrics}.
     */
    public Metrics metrics() {
        return Optional.ofNullable(this.settings.string("type"))
            .map(
                type -> {
                    final MetricsOutput output;
                    switch (type) {
                        case "log":
                            output = new MetricsLogOutput(LoggerFactory.getLogger(Metrics.class));
                            break;
                        case "asto":
                            output = new StorageMetricsOutput(
                                new SubStorage(
                                    new Key.From(".meta", "metrics"),
                                    new YamlStorage(this.settings.yamlMapping("storage")).storage()
                                )
                            );
                            break;
                        default:
                            throw new IllegalArgumentException(
                                String.format("Unsupported metrics type: %s", type)
                            );
                    }
                    final Metrics metrics = new InMemoryMetrics();
                    new MetricsPublisher(metrics, this.interval()).start(output);
                    return metrics;
                }
            ).orElseThrow(() -> new IllegalArgumentException("Metrics type is not specified"));
    }

    /**
     * Publishing interval, default interval is 5 seconds.
     * @return Interval
     * @checkstyle MagicNumberCheck (500 lines)
     */
    Duration interval() {
        return Optional.ofNullable(this.settings.string("interval"))
            .map(interval -> Duration.ofSeconds(Integer.parseInt(interval)))
            .orElse(Duration.ofSeconds(5));
    }
}
