/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.metrics.memory.InMemoryMetrics;
import com.artipie.metrics.publish.IntervalMetricOutput;
import com.artipie.metrics.publish.MetricsLogOutput;
import com.artipie.metrics.publish.MetricsOutputType;
import com.artipie.metrics.publish.MetricsPublisher;
import com.artipie.metrics.publish.StorageMetricsOutput;
import com.artipie.settings.YamlStorage;
import com.google.common.base.Strings;
import com.jcabi.log.Logger;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Metrics context.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 * @since 0.28.0
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ConfusingTernary"})
public final class MetricsContext {

    /**
     * Default publishing interval (sec).
     */
    private static final int DEFAULT_INTERVAL = 5;

    /**
     * Storage tag name.
     */
    private static final String STORAGE = "storage";

    /**
     * Metrics tag name.
     */
    private static final String METRICS = "metrics";

    /**
     * Map of metrics output settings.
     */
    private final Map<MetricsOutputType, YamlMapping> outputs;

    /**
     * Metrics periodical publisher.
     */
    private MetricsPublisher publisher;

    /**
     * Instance of metrics.
     */
    private Metrics metrics;

    /**
     * Ctor.
     */
    public MetricsContext() {
        this.outputs = new EnumMap<>(MetricsOutputType.class);
    }

    /**
     * Inits metrics context.
     *
     * @param settings Meta settings.
     */
    public void init(final YamlMapping settings) {
        final YamlSequence seq = settings.yamlSequence(MetricsContext.METRICS);
        if (seq != null) {
            seq.values().stream()
                .map(YamlNode::asMapping)
                .forEach(
                    mapping -> {
                        final String name = mapping.string("type");
                        if (!Strings.isNullOrEmpty(name)) {
                            final MetricsOutputType type = MetricsOutputType.valueOf(
                                name.toUpperCase(Locale.getDefault())
                            );
                            if (!this.outputs.containsKey(type)) {
                                this.outputs.put(type, mapping);
                            } else {
                                Logger.error(
                                    MetricsContext.class,
                                    "Metric with type %s already exists",
                                    name
                                );
                            }
                        } else {
                            throw new IllegalArgumentException("Empty metric type is not allowed");
                        }
                    }
            );
        }
        Logger.info(MetricsContext.class, "Metrics [outputs=%s]", this.outputs);
        if (!this.outputs.isEmpty()) {
            this.metrics = new InMemoryMetrics();
            final Collection<IntervalMetricOutput> list = this.outputs.entrySet()
                .stream()
                .filter(
                    entry -> entry.getKey().isInterval()
                )
                .map(
                    entry -> {
                        final IntervalMetricOutput res;
                        switch (entry.getKey()) {
                            case LOG:
                                res = new MetricsLogOutput(
                                    LoggerFactory.getLogger(Metrics.class),
                                    interval(entry.getValue())
                                );
                                break;
                            case ASTO:
                                res = new StorageMetricsOutput(
                                    metricsStorage(entry.getValue()
                                        .yamlMapping(MetricsContext.STORAGE)
                                    ),
                                    interval(entry.getValue())
                                );
                                break;
                            default:
                                throw new IllegalStateException(
                                    String.format(
                                        "%s is not interval type of metrics output",
                                        entry.getValue()
                                    )
                                );
                        }
                        return res;
                    }
                ).toList();
            this.publisher = new MetricsPublisher(this.metrics, list);
        }
    }

    /**
     * Metrics are enabled.
     *
     * @return True, if metrics are enabled.
     */
    public boolean enabled() {
        return !this.outputs.isEmpty();
    }

    /**
     * Log metrics output is enabled.
     *
     * @return True, if enabled.
     */
    public boolean logEnabled() {
        return this.outputs.containsKey(MetricsOutputType.LOG);
    }

    /**
     * Storage metrics output is enabled.
     *
     * @return True, if enabled.
     */
    public boolean storageEnabled() {
        return this.outputs.containsKey(MetricsOutputType.ASTO);
    }

    /**
     * Creates storage for metrics.
     *
     * @return Storage.
     */
    public Storage metricsStorage() {
        if (!this.storageEnabled()) {
            throw new IllegalStateException("Storage type metrics are not defined");
        }
        return metricsStorage(this.outputs.get(MetricsOutputType.ASTO)
            .yamlMapping(MetricsContext.STORAGE)
        );
    }

    /**
     * Prometheus metrics are enabled.
     *
     * @return True, if Prometheus metrics are enabled.
     */
    public boolean prometheusEnabled() {
        return this.outputs.containsKey(MetricsOutputType.PROMETHEUS);
    }

    /**
     * Vertx metrics are enabled.
     *
     * @return True, if vertx metrics are enabled.
     */
    public boolean vertxEnabled() {
        return this.outputs.containsKey(MetricsOutputType.VERTX);
    }

    /**
     * Endpoint to expose vertx metrics.
     *
     * @return Endpoint, if {@code null} then vertx metrics are not enabled.
     */
    public String vertxEndpoint() {
        String res = null;
        if (this.vertxEnabled()) {
            res = this.outputs.get(MetricsOutputType.VERTX).string("endpoint");
        }
        return res;
    }

    /**
     * Port to expose vertx metrics.
     *
     * @return Port, if -1 then vertx metrics are not enabled.
     */
    public int vertxPort() {
        int res = -1;
        if (this.vertxEnabled()) {
            res = Integer.parseInt(
                this.outputs.get(MetricsOutputType.VERTX).string("port")
            );
        }
        return res;
    }

    /**
     * Metrics.
     *
     * @return Instance of metrics.
     */
    public Metrics getMetrics() {
        if (!this.enabled()) {
            throw new IllegalStateException("Metrics are not defined");
        }
        return this.metrics;
    }

    /**
     * Starts output publisher.
     */
    public void start() {
        if (this.enabled()) {
            this.publisher.start();
        }
    }

    /**
     * Stops output publisher.
     */
    public void stop() {
        if (this.enabled()) {
            this.publisher.stop();
        }
    }

    @Override
    public String toString() {
        return this.outputs.toString();
    }

    /**
     * Creates storage for metrics.
     *
     * @param mapping Storage settings.
     * @return Storage.
     */
    private static Storage metricsStorage(final YamlMapping mapping) {
        return new SubStorage(
            new Key.From(".meta", MetricsContext.METRICS),
            new YamlStorage(mapping).storage()
        );
    }

    /**
     * Publishing interval, default interval is {@link #DEFAULT_INTERVAL}.
     *
     * @param mapping Yaml mapping to get the interval from.
     * @return Interval.
     */
    private static Duration interval(final YamlMapping mapping) {
        final Duration res;
        final String interval = mapping.string("interval");
        if (Strings.isNullOrEmpty(interval)) {
            res = Duration.ofSeconds(MetricsContext.DEFAULT_INTERVAL);
        } else {
            res = Duration.ofSeconds(Integer.parseInt(interval));
        }
        return res;
    }
}
