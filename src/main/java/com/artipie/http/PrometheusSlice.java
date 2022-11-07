/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.publish.MetricsOutput;
import com.artipie.metrics.publish.PrometheusOutput;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice that allows Prometheus to collect metrics (pull strategy).
 *
 * @see <a href="https://prometheus.io/docs/practices/instrumentation/#offline-processing"/>
 * @since 0.23
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class PrometheusSlice implements Slice {

    /**
     * Metrics.
     */
    private final Metrics metrics;

    /**
     * New slice with metrics.
     * @param metrics Metrics
     */
    public PrometheusSlice(final Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String ctype = new Accept(headers).values().get(0);
        try (StringWriter writer = new StringWriter()) {
            final MetricsOutput output = new PrometheusOutput(
                writer, ctype,
                new HashSet<>(
                    new RqParams(
                        new RequestLineFrom(line).uri()
                    ).values("name")
                )
            );
            this.metrics.publish(output);
            return new RsFull(
                RsStatus.OK,
                new Headers.From(new ContentType(ctype)),
                new Content.From(
                    writer.toString().getBytes(StandardCharsets.UTF_8)
                )
            );
        } catch (final IOException ioe) {
            throw new ArtipieIOException(ioe);
        }
    }
}
