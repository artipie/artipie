/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
 */
@SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidCatchingGenericException"})
final class SafeSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Wraps slice with safe decorator.
     * @param origin Origin slice
     */
    SafeSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        try {
            return new RsSafe(this.origin.response(line, headers, body));
        } catch (final Exception err) {
            Logger.error(this, "Failed to respond to request: %[exception]s", err);
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
                Logger.error(this, "Failed to send request to connection: %[exception]s", err);
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
