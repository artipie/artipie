/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Response with status.
 * @since 0.1
 */
public final class RsWithStatus implements Response {

    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Status code.
     */
    private final RsStatus status;

    /**
     * New response with status.
     * @param status Status code
     */
    public RsWithStatus(final RsStatus status) {
        this(StandardRs.EMPTY, status);
    }

    /**
     * Override status code for response.
     * @param origin Response to override
     * @param status Status code
     */
    public RsWithStatus(final Response origin, final RsStatus status) {
        this.origin = origin;
        this.status = status;
    }

    @Override
    public CompletionStage<Void> send(final Connection con) {
        return this.origin.send(new ConWithStatus(con, this.status));
    }

    @Override
    public String toString() {
        return String.format("RsWithStatus{status=%s, origin=%s}", this.status, this.origin);
    }

    /**
     * Connection with overridden status code.
     * @since 0.1
     */
    private static final class ConWithStatus implements Connection {

        /**
         * Origin connection.
         */
        private final Connection origin;

        /**
         * New status.
         */
        private final RsStatus status;

        /**
         * Override status code for connection.
         * @param origin Connection
         * @param status Code to override
         */
        ConWithStatus(final Connection origin, final RsStatus status) {
            this.origin = origin;
            this.status = status;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus ignored,
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return this.origin.accept(this.status, headers, body);
        }
    }
}
