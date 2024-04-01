/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.concurrent.CompletableFuture;
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
    public CompletableFuture<Response> response(final RequestLine line, final Headers head,
                                                final Content body) {
        final String method = line.method().value();
        final Counter.Builder requestCounter = Counter.builder("artipie.request.counter")
            .description("HTTP requests counter")
            .tag(MicrometerSlice.METHOD, method);
        final DistributionSummary requestBody = DistributionSummary.builder("artipie.request.body.size")
            .description("Request body size and chunks")
            .baseUnit(MicrometerSlice.BYTES)
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        final DistributionSummary responseBody = DistributionSummary.builder("artipie.response.body.size")
            .baseUnit(MicrometerSlice.BYTES)
            .description("Response body size and chunks")
            .tag(MicrometerSlice.METHOD, method)
            .register(this.registry);
        final Timer.Sample timer = Timer.start(this.registry);

        return this.origin.response(line, head, new MicrometerPublisher(body, requestBody))
            .thenCompose(response -> {
                requestCounter.tag(MicrometerSlice.STATUS, response.status().name())
                    .register(MicrometerSlice.this.registry).increment();
                return ResponseBuilder.from(response.status())
                    .headers(response.headers())
                    .body(new MicrometerPublisher(response.body(), responseBody))
                    .completedFuture();
            }).handle(
                (resp, err) -> {
                    CompletableFuture<Response> res;
                    String name = "artipie.slice.response";
                    if (err != null) {
                        name = String.format("%s.error", name);
                        timer.stop(this.registry.timer(name));
                        res = CompletableFuture.failedFuture(err);
                    } else {
                        timer.stop(this.registry.timer(name, MicrometerSlice.STATUS, resp.status().name()));
                        res = CompletableFuture.completedFuture(resp);
                    }
                    return res;
                }
            ).thenCompose(Function.identity());
    }
}
