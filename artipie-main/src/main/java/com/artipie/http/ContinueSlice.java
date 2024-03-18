/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Slice which sends {@code 100 Continue} status if expected before actual response.
 * See <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">rfc7231</a>.
 */
public final class ContinueSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Wrap slice with {@code continue} support.
     * @param origin Origin slice
     */
    public ContinueSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(RequestLine line, Headers headers,
                             Publisher<ByteBuffer> body) {
        final Response rsp;
        if (expectsContinue(headers)) {
            rsp = new ContinueResponse(
                new LazyResponse(() -> this.origin.response(line, headers, body))
            );
        } else {
            rsp = this.origin.response(line, headers, body);
        }
        return rsp;
    }

    /**
     * Check if request expects {@code continue} status to be sent before sending request body.
     * @param headers Request headers
     * @return True if expects
     */
    private static boolean expectsContinue(Headers headers) {
        return new RqHeaders(headers, "expect")
            .stream()
            .anyMatch(val -> val.equalsIgnoreCase("100-continue"));
    }

    /**
     * Response sends continue before origin response.
     * @since 0.19
     */
    private static final class ContinueResponse implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Wrap response with {@code continue} support.
         * @param origin Origin response
         */
        private ContinueResponse(final Response origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return connection.accept(RsStatus.CONTINUE, Headers.EMPTY, Flowable.empty())
                .thenCompose(none -> this.origin.send(connection));
        }
    }

    /**
     * Lazy response loaded on demand.
     * @since 0.19
     */
    private static final class LazyResponse implements Response {

        /**
         * Response supplier.
         */
        private final Supplier<Response> source;

        /**
         * New lazy response.
         * @param source Supplier
         */
        LazyResponse(final Supplier<Response> source) {
            this.source = source;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return this.source.get().send(connection);
        }
    }
}
