/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import org.reactivestreams.Publisher;

/**
 * Slice wrapper to generate JFR events for every the {@code response} method call.
 *
 * @since 0.28.0
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
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
        final Iterable<Map.Entry<String, String>> head,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rqLine = new RequestLineFrom(line);
        final SliceResponseEvent event = new SliceResponseEvent();
        event.method = rqLine.method().value();
        event.path = rqLine.uri().getPath();
        event.begin();
        final Response res = this.original.response(
            line,
            head,
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
                event.responseChunks = chunks;
                event.responseSize = size;
                event.commit();
            }
        );
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
