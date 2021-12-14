/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;
import com.artipie.metrics.publish.PrometheusOutput;
import com.google.common.base.Splitter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.reactivestreams.Publisher;

/**
 * Slice that allows Prometheus to collect metrics (pull strategy).
 * @see <a href="https://prometheus.io/docs/practices/instrumentation/#offline-processing"/>
 * @since 0.23
 * @todo #889:30min Test that {@link PromuSlice}'s response is correct.
 *  We implemented {@link PromuSlice} in a way that Prometheus can pull
 *  model via that. Now, we should check that its response is correct.
 *  We should also add an integration test to be sure that configuration
 *  is taken into account.
 */
public final class PromuSlice implements Slice {

    /**
     * Metrics.
     */
    private final Metrics metrics;

    /**
     * New slice with metrics.
     * @param metrics Metrics
     */
    public PromuSlice(final Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String ctype = new Accept(headers).values().get(0);
        final StringWriter writer = new StringWriter();
        final Set<String> filters = PromuSlice.params(line, "name");
        final MetricsOutput output = new PrometheusOutput(writer, ctype, filters);
        this.metrics.publish(output);
        return new RsFull(
            RsStatus.OK,
            new Headers.From(new ContentType(ctype)),
            new Content.From(
                String.valueOf(writer.getBuffer()).getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    /**
     * Get all params with name in query.
     * @param query Query
     * @param name Name
     * @return All params with this name
     */
    private static Set<String> params(final String query, final String name) {
        return StreamSupport.stream(
            Splitter.on("&").omitEmptyStrings().split(query).spliterator(),
            false
        ).flatMap(
            param -> {
                final String prefix = String.format("%s=", name);
                final Stream<String> value;
                if (param.startsWith(prefix)) {
                    value = Stream.of(param.substring(prefix.length()));
                } else {
                    value = Stream.empty();
                }
                return value;
            }
        ).collect(Collectors.toSet());
    }
}
