/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.BaseResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Head slice for Maven proxy.
 * @since 0.5
 */
final class HeadProxySlice implements Slice {

    /**
     * Client slice.
     */
    private final Slice client;

    /**
     * New slice for {@code HEAD} requests.
     * @param client HTTP client slice
     */
    HeadProxySlice(final Slice client) {
        this.client = client;
    }

    @Override
    public Response response(final RequestLine line, final Headers headers,
                             final Content body) {
        final CompletableFuture<Response> promise = new CompletableFuture<>();
        this.client.response(line, Headers.EMPTY, Content.EMPTY).send(
            (status, rsheaders, rsbody) -> {
                promise.complete(BaseResponse.from(status).headers(rsheaders));
                return CompletableFuture.allOf();
            }
        );
        return new AsyncResponse(promise);
    }
}
