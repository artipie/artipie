/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Calculated uploaded and downloaded body size for all requests.
 * @since 0.28
 */
public final class MicrometerSlice implements Slice {

    /**
     * Summary tag route.
     */
    private static final String ROUTE = "route";

    /**
     * Tag method.
     */
    private static final String METHOD = "method";

    /**
     * Summary unit.
     */
    private static final String BYTES = "bytes";

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Micrometer registry.
     */
    private final MeterRegistry registry;

    /**
     * Update traffic metrics on requests and responses.
     * @param origin Origin slice to decorate
     */
    public MicrometerSlice(final Slice origin) {
        this(origin, BackendRegistries.getDefaultNow());
    }

    /**
     * Ctor.
     * @param origin Origin slice to decorate
     * @param registry Micrometer registry
     */
    public MicrometerSlice(final Slice origin, final MeterRegistry registry) {
        this.origin = origin;
        this.registry = registry;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> head,
        final Publisher<ByteBuffer> body) {
        final RequestLineFrom rqline = new RequestLineFrom(line);
        final String path = rqline.uri().getPath();
        final String method = rqline.method().value();
        final Counter.Builder cnt = Counter.builder("artipie.request.counter")
            .tag(MicrometerSlice.ROUTE, path).tag(MicrometerSlice.METHOD, method);
        final DistributionSummary rqbody = DistributionSummary.builder("artipie.request.body.size")
            .baseUnit(MicrometerSlice.BYTES)
            .tag(MicrometerSlice.ROUTE, path)
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        final DistributionSummary rsbody = DistributionSummary.builder("artipie.response.body.size")
            .baseUnit(MicrometerSlice.BYTES)
            .tag(MicrometerSlice.ROUTE, path)
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        return new MicrometerResponse(
            this.origin.response(line, head, new MicrometerPublisher(body, rqbody)), rsbody, cnt
        );
    }

    /**
     * Response which sends itself to connection with metrics.
     * @since 0.10
     */
    private final class MicrometerResponse implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Micrometer distribution summary.
         */
        private final DistributionSummary summary;

        /**
         * Micrometer requests counter.
         */
        private final Counter.Builder counter;

        /**
         * Wraps response.
         *
         * @param response Origin response
         * @param summary Micrometer distribution summary
         * @param counter Micrometer requests counter
         */
        MicrometerResponse(final Response response, final DistributionSummary summary,
            final Counter.Builder counter) {
            this.origin = response;
            this.summary = summary;
            this.counter = counter;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return this.origin.send(
                new MicrometerConnection(connection, this.summary, this.counter)
            );
        }

        /**
         * Response connection which updates metrics on accept.
         * @since 0.10
         */
        private final class MicrometerConnection implements Connection {

            /**
             * Origin connection.
             */
            private final Connection origin;

            /**
             * Micrometer distribution summary.
             */
            private final DistributionSummary summary;

            /**
             * Micrometer requests counter.
             */
            private final Counter.Builder counter;

            /**
             * Wrap connection.
             *
             * @param origin Origin connection
             * @param summary Micrometer distribution summary
             * @param counter Micrometer requests counter
             */
            MicrometerConnection(final Connection origin, final DistributionSummary summary,
                final Counter.Builder counter) {
                this.origin = origin;
                this.summary = summary;
                this.counter = counter;
            }

            @Override
            public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
                final Publisher<ByteBuffer> body) {
                this.counter.tag("status", status.name()).register(MicrometerSlice.this.registry)
                    .increment();
                return this.origin.accept(
                    status, headers, new MicrometerPublisher(body, this.summary)
                );
            }
        }
    }
}
