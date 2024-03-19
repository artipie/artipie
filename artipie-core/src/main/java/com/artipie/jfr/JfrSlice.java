/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

/**
 * Slice wrapper to generate JFR events for every the {@code response} method call.
 */
public final class JfrSlice implements Slice {

    private final Slice original;

    /**
     * @param original Original slice.
     */
    public JfrSlice(final Slice original) {
        this.original = original;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final SliceResponseEvent event = new SliceResponseEvent();
        if (event.isEnabled()) {
            return this.wrapResponse(line, headers, body, event);
        }
        return this.original.response(line, headers, body);
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
        final RequestLine line,
        final Headers headers,
        final Content body,
        final SliceResponseEvent event
    ) {
        event.begin();
        final Response res = this.original.response(
            line,
            headers,
            new Content.From(
                new ChunksAndSizeMetricsPublisher(
                    body,
                    (chunks, size) -> {
                        event.requestChunks = chunks;
                        event.requestSize = size;
                    }
                )
            )
        );
        return new JfrResponse(
            res,
            (chunks, size) -> {
                event.end();
                if (event.shouldCommit()) {
                    event.method = line.method().value();
                    event.path = line.uri().getPath();
                    event.headers = headers.asString();
                    event.responseChunks = chunks;
                    event.responseSize = size;
                    event.commit();
                }
            }
        );
    }

    /**
     * Response JFR wrapper.
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
            final Content body
        ) {
            return this.original.accept(
                status,
                headers,
                new Content.From(new ChunksAndSizeMetricsPublisher(body, this.callback))
            );
        }
    }

}
