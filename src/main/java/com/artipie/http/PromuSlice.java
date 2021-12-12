/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentAs;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.google.common.base.Splitter;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.exporter.common.TextFormat;
import io.reactivex.Observable;
import org.reactivestreams.Publisher;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Slice that allows Prometheus to collect metrics (pull way).
 * @see <a href="https://prometheus.io/docs/practices/instrumentation/#offline-processing"/>
 * @since 0.23
 */
public final class PromuSlice implements Slice {

    /**
     * Storage with metrics.
     */
    private final Storage storage;

    /**
     * New slice with metrics.
     * @param storage Storage with metrics
     */
    public PromuSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.storage);
        final CollectorRegistry registry = new CollectorRegistry();
        return new AsyncResponse(
            rxsto.list(Key.ROOT)
                .flatMapObservable(Observable::fromIterable)
                .flatMapSingle(
                    key -> rxsto.value(key).to(ContentAs.LONG).map(
                        val -> {
                            // @see https://github.com/prometheus/client_java#counter
                            Counter.build()
                                .name(key.string())
                                .register(registry)
                                .inc(val);
                            return registry;
                        }
                    )
                ).reduce(
                    (regl, regr) -> registry
                )
                .map(
                    reg -> {
                        // @see https://github.com/prometheus/client_java/blob/65ca8bd19382c4f35f7f8d10e2cc462faf3adf3c/simpleclient_vertx/src/main/java/io/prometheus/client/vertx/MetricsHandler.java#L73
                        final String ctype = new Accept(headers).values().get(0);
                        final StringWriter writer = new StringWriter();
                        TextFormat.writeFormat(
                            ctype,
                            writer,
                            reg.filteredMetricFamilySamples(
                                PromuSlice.params(line, "name")
                            )
                        );
                        return new RsFull(
                            RsStatus.OK,
                            new Headers.From(new ContentType(ctype)),
                            new Content.From(
                                String.valueOf(writer.getBuffer()).getBytes(StandardCharsets.UTF_8)
                            )
                        );
                    }
                ).toSingle()
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
