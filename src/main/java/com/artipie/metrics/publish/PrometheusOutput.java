/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics.publish;

import com.artipie.ArtipieException;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * Prometheus metrics output.
 * @since 0.23
 */
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
     * Registry.
     */
    private final CollectorRegistry registry;

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
        this.registry = new CollectorRegistry();
    }

    @Override
    public void counters(final Map<String, Long> data) {
        this.write(data);
    }

    @Override
    public void gauges(final Map<String, Long> data) {
        this.write(data);
    }

    /**
     * Writes metrics in Prometheus format.
     * @param data Metrics to write
     */
    private void write(final Map<String, Long> data) {
        for (final Map.Entry<String, Long> metric : data.entrySet()) {
            // @checkstyle MethodBodyCommentsCheck (1 line)
            // @see https://github.com/prometheus/client_java#counter
            Counter.build()
                .name(metric.getKey())
                .register(this.registry)
                .inc(metric.getValue());
        }
        try {
            // @checkstyle MethodBodyCommentsCheck (3 lines)
            // @checkstyle LineLengthCheck (1 line)
            // @see https://github.com/prometheus/client_java/blob/65ca8bd19382c4f35f7f8d10e2cc462faf3adf3c/simpleclient_vertx/src/main/java/io/prometheus/client/vertx/MetricsHandler.java#L73
            TextFormat.writeFormat(
                this.mtype,
                this.writer,
                this.registry.filteredMetricFamilySamples(this.filters)
            );
        } catch (final IOException ioe) {
            throw new ArtipieException(ioe);
        }
    }
}
