/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.metrics.memory.InMemoryMetrics;
import com.artipie.metrics.publish.MetricsLogOutput;
import com.artipie.metrics.publish.MetricsPublisher;
import com.artipie.metrics.publish.StorageMetricsOutput;
import com.artipie.settings.YamlStorage;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

/**
 * Metrics from config.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class MetricsFromConfig {

    /**
     * Key word to identify Prometheus.
     */
    public static final String PROMETHEUS = "prometheus";

    /**
     * Log metrics type.
     */
    private static final String LOG = "log";

    /**
     * Metrics yaml type field name.
     */
    private static final String TYPE = "type";

    /**
     * Metrics section from settings.
     */
    private final YamlSequence settings;

    /**
     * Ctor.
     * @param metrics Yaml settings
     */
    public MetricsFromConfig(final YamlSequence metrics) {
        this.settings = metrics;
    }

    /**
     * Returns {@link Metrics} instance according to configuration and starts
     * publishers for metrics types "log" and "asto".
     * @return Instance of {@link Metrics}.
     */
    public Metrics metrics() {
        final Metrics metrics = new InMemoryMetrics();
        this.settings.values().stream().filter(
            item -> MetricsFromConfig.LOG.equals(item.asMapping().string(MetricsFromConfig.TYPE))
                || "asto".equals(item.asMapping().string(MetricsFromConfig.TYPE))
        ).forEach(
            item -> {
                final String type = item.asMapping().string(MetricsFromConfig.TYPE);
                if (MetricsFromConfig.LOG.equals(type)) {
                    new MetricsPublisher(metrics, MetricsFromConfig.interval(item))
                        .start(new MetricsLogOutput(LoggerFactory.getLogger(Metrics.class)));
                } else {
                    new MetricsPublisher(metrics, MetricsFromConfig.interval(item)).start(
                        new StorageMetricsOutput(
                            new SubStorage(
                                new Key.From(".meta", "metrics"),
                                new YamlStorage(item.asMapping().yamlMapping("storage")).storage()
                            )
                        )
                    );
                }
            }
        );
        return metrics;
    }

    /**
     * Obtains, if configured, path and port to expose Vert.x metrics.
     * @return Path and port to expose Vert.x metrics on
     */
    public Optional<Pair<String, Integer>> vertxMetricsConf() {
        return this.settings.values().stream().filter(
            item -> "vertx".equals(item.asMapping().string(MetricsFromConfig.TYPE))
        ).findFirst().map(YamlNode::asMapping).map(
            map -> new ImmutablePair<>(
                Objects.requireNonNull(map.string("path")),
                map.integer("port")
            )
        );
    }

    /**
     * Publishing interval, default interval is 5 seconds.
     * @param node Yaml node to get the interval from
     * @return Interval
     * @checkstyle MagicNumberCheck (500 lines)
     */
    static Duration interval(final YamlNode node) {
        return Optional.ofNullable(node.asMapping().string("interval"))
            .map(interval -> Duration.ofSeconds(Integer.parseInt(interval)))
            .orElse(Duration.ofSeconds(5));
    }
}
