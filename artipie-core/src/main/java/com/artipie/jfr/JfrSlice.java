/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Slice wrapper to generate JFR events for every the {@code response} method call.
 *
 * @since 0.28.0
 */
public final class JfrSlice implements Slice {

    /**
     * Original slice.
     */
    private final Slice original;

    /**
     * Ctor.
     *
     * @param original Original slice.
     */
    public JfrSlice(final Slice original) {
        this.original = original;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response res;
        final SliceResponseEvent event = new SliceResponseEvent();
        if (event.isEnabled()) {
            res = this.wrapResponse(line, headers, body, event);
        } else {
            res = this.original.response(line, headers, body);
        }
        return res;
    }

    /**
     * Executes request and fills an event data.
     *
     * @param line The request line
     * @param headers The request headers
     * @param body The request body
     * @param event JFR event
     * @return The response.
     */
    private Response wrapResponse(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body,
        final SliceResponseEvent event
    ) {
        event.begin();
        final Response res = this.original.response(
            line,
            headers,
            new ChunksAndSizeMetricsPublisher(
                body,
                (chunks, size) -> {
                    event.requestChunks = chunks;
                    event.requestSize = size;
                }
            )
        );
        return new JfrResponse(
            res,
            (chunks, size) -> {
                event.end();
                if (event.shouldCommit()) {
                    final RequestLineFrom rqLine = new RequestLineFrom(line);
                    event.method = rqLine.method().value();
                    event.path = rqLine.uri().getPath();
                    event.headers = JfrSlice.headersAsString(headers);
                    event.responseChunks = chunks;
                    event.responseSize = size;
                    event.commit();
                }
            }
        );
    }

    /**
     * Headers to String.
     *
     * @param headers Headers
     * @return String
     */
    private static String headersAsString(final Iterable<Map.Entry<String, String>> headers) {
        return StreamSupport.stream(headers.spliterator(), false)
            .map(entry -> entry.getKey() + '=' + entry.getValue())
            .collect(Collectors.joining(";"));
    }

    /**
     * Response JFR wrapper.
     *
     * @since 0.28.0
     */
    private static final class JfrResponse implements Response {

        /**
         * Original response.
         */
        private final Response original;

        /**
         * Callback consumer.
         */
        private final BiConsumer<Integer, Long> callback;

        /**
         * Ctor.
         *
         * @param original Original response.
         * @param callback Callback consumer.
         */
        JfrResponse(final Response original, final BiConsumer<Integer, Long> callback) {
            this.original = original;
            this.callback = callback;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return this.original.send(
                new JfrConnection(connection, this.callback)
            );
        }
    }

    /**
     * Connection JFR wrapper.
     *
     * @since 0.28.0
     */
    private static final class JfrConnection implements Connection {

        /**
         * Original connection.
         */
        private final Connection original;

        /**
         * Callback consumer.
         */
        private final BiConsumer<Integer, Long> callback;

        /**
         * Ctor.
         *
         * @param original Original connection.
         * @param callback Callback consumer.
         */
        JfrConnection(
            final Connection original,
            final BiConsumer<Integer, Long> callback
        ) {
            this.original = original;
            this.callback = callback;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> body
        ) {
            return this.original.accept(
                status,
                headers,
                new ChunksAndSizeMetricsPublisher(body, this.callback)
            );
        }
    }

}
