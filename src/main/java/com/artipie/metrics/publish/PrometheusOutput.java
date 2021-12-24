/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.ArtipieException;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Prometheus metrics output.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class PrometheusOutput implements MetricsOutput {

    /**
     * Writer.
     */
    private final Writer writer;

    /**
     * Content's mime type.
     */
    private final String mtype;

    /**
     * Metric names to match with.
     */
    private final Set<String> filters;

    /**
     * Ctor.
     * @param writer Writer
     * @param mtype Output content mime type
     * @param filters Metric names to select
     */
    public PrometheusOutput(final Writer writer, final String mtype,
        final Set<String> filters) {
        this.writer = writer;
        this.mtype = mtype;
        this.filters = filters;
    }

    @Override
    public void counters(final Map<String, Long> data) {
        if (!data.isEmpty()) {
            // @checkstyle MethodBodyCommentsCheck (1 line)
            // @see https://github.com/prometheus/client_java#counter
            final CollectorRegistry registry = new CollectorRegistry();
            for (final Map.Entry<String, Long> metric : data.entrySet()) {
                Counter.build(name(metric.getKey()), help(metric.getKey()))
                    .register(registry)
                    .inc(metric.getValue());
            }
            this.write(registry);
        }
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        if (!data.isEmpty()) {
            // @checkstyle MethodBodyCommentsCheck (1 line)
            // @see https://github.com/prometheus/client_java#gauge
            final CollectorRegistry registry = new CollectorRegistry();
            for (final Map.Entry<String, Long> metric : data.entrySet()) {
                Gauge.build(name(metric.getKey()), help(metric.getKey()))
                    .register(registry)
                    .set(metric.getValue());
            }
            this.write(registry);
        }
    }

    /**
     * Writes metrics in Prometheus format.
     * @param registry Collector registry
     */
    private void write(final CollectorRegistry registry) {
        try {
            // @checkstyle MethodBodyCommentsCheck (3 lines)
            // @checkstyle LineLengthCheck (1 line)
            // @see https://github.com/prometheus/client_java/blob/65ca8bd19382c4f35f7f8d10e2cc462faf3adf3c/simpleclient_vertx/src/main/java/io/prometheus/client/vertx/MetricsHandler.java#L73
            TextFormat.writeFormat(
                TextFormat.chooseContentType(this.mtype),
                this.writer,
                registry.filteredMetricFamilySamples(this.filters)
            );
        } catch (final IOException ioe) {
            throw new ArtipieException(ioe);
        }
    }

    /**
     * Normalizes metrics name.
     * @param name Name
     * @return Normalized name
     */
    private static String name(final String name) {
        return name.replaceAll("\\.", "_");
    }

    /**
     * Builds help from name.
     * @param name Name
     * @return Help
     */
    private static String help(final String name) {
        return StringUtils.capitalize(
            name.replaceAll("\\.", " ")
        );
    }
}
