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
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;

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
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final CompletableFuture<Response> promise = new CompletableFuture<>();
        this.client.response(line, Headers.EMPTY, Content.EMPTY).send(
            (status, rsheaders, rsbody) -> {
                promise.complete(new RsWithHeaders(new RsWithStatus(status), rsheaders));
                return CompletableFuture.allOf();
            }
        );
        return new AsyncResponse(promise);
    }
}
