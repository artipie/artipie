/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.http.Response;

import java.util.concurrent.CompletableFuture;

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
    private final CompletableFuture<Response> fut;

    /**
     * Response transformation.
     */
    private final Transformation<T> transform;

    /**
     * @param fut Response future.
     * @param transform Response transformation.
     */
    ResponseSink(CompletableFuture<Response> fut, final Transformation<T> transform) {
        this.fut = fut;
        this.transform = transform;
    }

    /**
     * Transform result into object.
     *
     * @return Result object.
     */
    public CompletableFuture<T> result() {
        return fut.thenCompose(this.transform::transform);
    }

    /**
     * Transformation that transforms response into result object.
     *
     * @param <T> Result object type.
     */
    interface Transformation<T> {

        /**
         * Transform response into an object.
         *
         * @param response Response.
         */
        CompletableFuture<T> transform(Response response);
    }
}
