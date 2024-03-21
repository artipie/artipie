/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.jcabi.log.Logger;

import java.util.concurrent.CompletionStage;

/**
 * Slice which handles all exceptions and respond with 500 error in that case.
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
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
    public Response response(RequestLine line, Headers headers, Content body) {
        try {
            return new RsSafe(this.origin.response(line, headers, body));
        } catch (final Exception err) {
            Logger.error(this, "Failed to respond to request: %[exception]s", err);
            return BaseResponse.internalError()
                .textBody("Failed to respond to request: " + err.getMessage());
        }
    }

    /**
     * Safe response, catches exceptions from underlying reponse calls and respond with 500 error.
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
                return BaseResponse.internalError()
                    .textBody("Failed to send request to connection: " + err.getMessage())
                    .send(connection);
            }
        }
    }
}
