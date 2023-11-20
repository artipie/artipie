/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import org.reactivestreams.Publisher;

/**
 * Slice that logs incoming requests and outgoing responses.
 *
 * @since 0.8
 * @checkstyle IllegalCatchCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public final class LoggingSlice implements Slice {

    /**
     * Logging level.
     */
    private final Level level;

    /**
     * Delegate slice.
     */
    private final Slice slice;

    /**
     * Ctor.
     *
     * @param slice Slice.
     */
    public LoggingSlice(final Slice slice) {
        this(Level.FINE, slice);
    }

    /**
     * Ctor.
     *
     * @param level Logging level.
     * @param slice Slice.
     */
    public LoggingSlice(final Level level, final Slice slice) {
        this.level = level;
        this.slice = slice;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final StringBuilder msg = new StringBuilder(">> ").append(line);
        LoggingSlice.append(msg, headers);
        Logger.log(this.level, this.slice, msg.toString());
        return connection -> {
            try {
                return this.slice.response(line, headers, body)
                    .send(new LoggingConnection(connection))
                    .handle(
                        (value, throwable) -> {
                            final CompletableFuture<Void> result = new CompletableFuture<>();
                            if (throwable == null) {
                                result.complete(value);
                            } else {
                                this.log(throwable);
                                result.completeExceptionally(throwable);
                            }
                            return result;
                        }
                    )
                    .thenCompose(Function.identity());
            } catch (final Exception ex) {
                this.log(ex);
                throw ex;
            }
        };
    }

    /**
     * Writes throwable to logger.
     *
     * @param throwable Throwable to be logged.
     */
    private void log(final Throwable throwable) {
        Logger.log(this.level, this.slice, "Failure: %[exception]s", throwable);
    }

    /**
     * Append headers to {@link StringBuilder}.
     *
     * @param builder Target {@link StringBuilder}.
     * @param headers Headers to be appended.
     */
    private static void append(
        final StringBuilder builder,
        final Iterable<Map.Entry<String, String>> headers
    ) {
        for (final Map.Entry<String, String> header : headers) {
            builder.append('\n').append(header.getKey()).append(": ").append(header.getValue());
        }
    }

    /**
     * Connection logging response prior to sending.
     *
     * @since 0.8
     */
    private final class LoggingConnection implements Connection {

        /**
         * Delegate connection.
         */
        private final Connection connection;

        /**
         * Ctor.
         *
         * @param connection Delegate connection.
         */
        private LoggingConnection(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> body
        ) {
            final StringBuilder msg = new StringBuilder("<< ").append(status);
            LoggingSlice.append(msg, headers);
            Logger.log(LoggingSlice.this.level, LoggingSlice.this.slice, msg.toString());
            return this.connection.accept(status, headers, body);
        }
    }
}
