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
package com.artipie.http;

import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Slice which handles all exceptions and respond with 500 error in that case.
 * @since 0.9
 * @checkstyle IllegalCatchCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidCatchingGenericException"})
final class SliceSafe implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Wraps slice with safe decorator.
     * @param origin Origin slice
     */
    SliceSafe(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        try {
            return new RsSafe(this.origin.response(line, headers, body));
        } catch (final Exception err) {
            Logger.error(this, "Failed to respond to request: %[exception]", err);
            return new RsWithBody(
                new RsWithStatus(RsStatus.INTERNAL_ERROR),
                String.format(
                    "Failed to respond to request: %s",
                    err.getMessage()
                ),
                StandardCharsets.UTF_8
            );
        }
    }

    /**
     * Safe response, catches exceptions from underlying reponse calls and respond with 500 error.
     * @since 0.9
     */
    private static final class RsSafe implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Wraps response with safe decorator.
         * @param origin Origin response
         */
        RsSafe(final Response origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            try {
                return this.origin.send(connection);
            } catch (final Exception err) {
                Logger.error(this, "Failed to send request to connection: %[exception]", err);
                return new RsWithBody(
                    new RsWithStatus(RsStatus.INTERNAL_ERROR),
                    String.format(
                        "Failed to send request to connection: %s",
                        err.getMessage()
                    ),
                    StandardCharsets.UTF_8
                ).send(connection);
            }
        }
    }
}
