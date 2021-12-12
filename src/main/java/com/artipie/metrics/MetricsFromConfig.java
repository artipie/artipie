/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
                        case "promu":
                            output = new StorageMetricsOutput(
                                new SubStorage(
                                    new Key.From(".meta", "metrics"),
                                    new YamlStorage(this.settings.yamlMapping("cache_storage")).storage()
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
