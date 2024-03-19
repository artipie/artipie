/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Calculated uploaded and downloaded body size for all requests.
 */
public final class MicrometerSlice implements Slice {

    /**
     * Tag method.
     */
    private static final String METHOD = "method";

    /**
     * Summary unit.
     */
    private static final String BYTES = "bytes";

    /**
     * Tag response status.
     */
    private static final String STATUS = "status";

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
    public Response response(final RequestLine line, final Headers head,
                             final Content body) {
        final String method = line.method().value();
        final Counter.Builder cnt = Counter.builder("artipie.request.counter")
            .description("HTTP requests counter")
            .tag(MicrometerSlice.METHOD, method);
        final DistributionSummary rqbody = DistributionSummary.builder("artipie.request.body.size")
            .description("Request body size and chunks")
            .baseUnit(MicrometerSlice.BYTES)
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        final DistributionSummary rsbody = DistributionSummary.builder("artipie.response.body.size")
            .baseUnit(MicrometerSlice.BYTES)
            .description("Response body size and chunks")
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        final Timer.Sample timer = Timer.start(this.registry);
        return new MicrometerResponse(
            this.origin.response(line, head, new MicrometerPublisher(body, rqbody)),
            rsbody, cnt, timer
        );
    }

    /**
     * Handle completion of some action by registering the timer.
     * @param name Timer name
     * @param timer The timer
     * @param status Response status
     * @return Completable action
     */
    private BiFunction<Void, Throwable, CompletionStage<Void>> handleWithTimer(
        final String name, final Timer.Sample timer, final Optional<String> status
    ) {
        return (ignored, err) -> {
            CompletionStage<Void> res = CompletableFuture.allOf();
            String copy = name;
            if (err != null) {
                copy = String.format("%s.error", name);
                res = CompletableFuture.failedFuture(err);
            }
            if (status.isPresent()) {
                timer.stop(this.registry.timer(copy, MicrometerSlice.STATUS, status.get()));
            } else {
                timer.stop(this.registry.timer(copy));
            }
            return res;
        };
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
         * Timer sample to measure slice.response method execution time.
         */
        private final Timer.Sample sample;

        /**
         * Wraps response.
         *
         * @param response Origin response
         * @param summary Micrometer distribution summary
         * @param counter Micrometer requests counter
         * @param sample Timer sample to measure slice.response method execution time
         */
        MicrometerResponse(final Response response, final DistributionSummary summary,
            final Counter.Builder counter, final Timer.Sample sample) {
            this.origin = response;
            this.summary = summary;
            this.counter = counter;
            this.sample = sample;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            final Timer.Sample timer = Timer.start(MicrometerSlice.this.registry);
            return this.origin.send(
                new MicrometerConnection(
                    connection, this.summary, this.counter, this.sample
                )
            ).handle(
                MicrometerSlice.this.handleWithTimer(
                    "artipie.response.send", timer, Optional.empty()
                )
            ).thenCompose(Function.identity());
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
             * Timer sample to measure slice.response method execution time.
             */
            private final Timer.Sample sample;

            /**
             * Wrap connection.
             *
             * @param origin Origin connection
             * @param summary Micrometer distribution summary
             * @param counter Micrometer requests counter
             * @param sample Timer sample to measure slice.response method execution time
             */
            MicrometerConnection(final Connection origin, final DistributionSummary summary,
                final Counter.Builder counter, final Timer.Sample sample) {
                this.origin = origin;
                this.summary = summary;
                this.counter = counter;
                this.sample = sample;
            }

            @Override
            public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
                final Content body) {
                this.counter.tag(MicrometerSlice.STATUS, status.name())
                    .register(MicrometerSlice.this.registry).increment();
                final Timer.Sample timer = Timer.start(MicrometerSlice.this.registry);
                return this.origin.accept(
                    status, headers, new MicrometerPublisher(body, this.summary)
                ).handle(
                    MicrometerSlice.this.handleWithTimer(
                        "artipie.connection.accept", timer, Optional.of(status.name())
                    )
                ).thenCompose(Function.identity()).handle(
                    MicrometerSlice.this.handleWithTimer(
                        "artipie.slice.response", this.sample, Optional.of(status.name())
                    )
                ).thenCompose(Function.identity());
            }
        }
    }
}
