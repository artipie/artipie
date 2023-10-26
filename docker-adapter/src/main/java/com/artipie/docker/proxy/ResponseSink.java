/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Sink that accepts response data (status, headers and body) and transforms it into result object.
 *
 * @param <T> Result object type.
 * @since 0.10
 */
final class ResponseSink<T> {

    /**
     * Response.
     */
    private final Response response;

    /**
     * Response transformation.
     */
    private final Transformation<T> transform;

    /**
     * Ctor.
     *
     * @param response Response.
     * @param transform Response transformation.
     */
    ResponseSink(final Response response, final Transformation<T> transform) {
        this.response = response;
        this.transform = transform;
    }

    /**
     * Transform result into object.
     *
     * @return Result object.
     */
    public CompletionStage<T> result() {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        return this.response.send(
            (status, headers, body) -> this.transform.transform(status, headers, body)
                .thenAccept(promise::complete)
        ).thenCompose(nothing -> promise);
    }

    /**
     * Transformation that transforms response into result object.
     *
     * @param <T> Result object type.
     * @since 0.10
     */
    interface Transformation<T> {

        /**
         * Transform response into an object.
         *
         * @param status Response status.
         * @param headers Response headers.
         * @param body Response body.
         * @return Completion stage for transformation.
         */
        CompletionStage<T> transform(RsStatus status, Headers headers, Publisher<ByteBuffer> body);
    }
}
