/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Slice implementation which measures request and response time.
 * @since 0.10
 */
public final class MeasuredSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Wraps slice with measured decorator.
     * @param origin Origin slice to measure
     */
    public MeasuredSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        final Response res;
        if (Logger.isDebugEnabled(MeasuredSlice.class)) {
            res = this.debugResponse(line, headers, body);
        } else {
            res = this.origin.response(line, headers, body);
        }
        return res;
    }

    /**
     * Debug response.
     * @param line Request line
     * @param headers Headers
     * @param body Body
     * @return Response
     */
    private Response debugResponse(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        final long start = System.nanoTime();
        final StringBuilder message = new StringBuilder();
        message.append(line).append(": ");
        final Response response = this.origin.response(line, headers, body);
        message.append(String.format("response=%s ", millisMessage(start)));
        return new MeasuredResponse(response, message, start);
    }

    /**
     * Amount of milliseconds from time.
     * @param from Starting point in nanoseconds
     * @return Formatted milliseconds message
     */
    private static String millisMessage(final long from) {
        // @checkstyle MagicNumberCheck (1 line)
        return String.format("%dms", (System.nanoTime() - from) / 1_000_000);
    }

    /**
     * Measured response wrapper.
     * @since 0.10
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private static final class MeasuredResponse implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Log message builder.
         */
        private final StringBuilder message;

        /**
         * Start time.
         */
        private final long start;

        /**
         * New measured response.
         * @param origin Origin response
         * @param message Log message builder
         * @param start Start time nanoseconds
         */
        MeasuredResponse(final Response origin, final StringBuilder message,
            final long start) {
            this.origin = origin;
            this.message = message;
            this.start = start;
        }

        @Override
        public CompletionStage<Void> send(final Connection con) {
            final long send = System.nanoTime();
            return this.origin.send(new MeasuredConnection(con, this.message)).thenRun(
                () -> {
                    final String log;
                    synchronized (this.message) {
                        log = this.message.append(String.format("send=%s ", millisMessage(send)))
                            .append(String.format("total=%s", millisMessage(this.start)))
                            .toString();
                    }
                    Logger.debug(MeasuredSlice.class, log);
                }
            );
        }
    }

    /**
     * Measured connection wrapper.
     * @since 0.10
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private static final class MeasuredConnection implements Connection {

        /**
         * Origin connection.
         */
        private final Connection origin;

        /**
         * Log message builder.
         */
        private final StringBuilder message;

        /**
         * New measured connection.
         * @param origin Origin connection
         * @param message Log message builder
         */
        private MeasuredConnection(final Connection origin, final StringBuilder message) {
            this.origin = origin;
            this.message = message;
        }

        @Override
        public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
            final Publisher<ByteBuffer> body) {
            final long time = System.nanoTime();
            return this.origin.accept(status, headers, body).thenRun(
                () -> {
                    synchronized (this.message) {
                        this.message.append(String.format("accept=%s ", millisMessage(time)));
                    }
                }
            );
        }
    }
}
