/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;

/**
 * Slice which sends {@code 100 Continue} status if expected before actual response.
 * See <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">rfc7231</a>.
 * @since 0.19
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
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
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
    private static boolean expectsContinue(final Iterable<Map.Entry<String, String>> headers) {
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
