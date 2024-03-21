/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Response that caches origin response once it first sent and can replay it many times.
 * <p>It can be useful when testing one response against multiple matchers, and response
 * from slice should be called only once.</p>
 */
public final class CachedResponse implements Response {

    /**
     * Origin response.
     */
    private final Response origin;

    private final StatefulConnection connection;

    /**
     * Wraps response with stateful connection.
     * @param origin Origin response
     */
    public CachedResponse(Response origin) {
        this.origin = origin;
        this.connection = new StatefulConnection();
    }

    @Override
    public CompletionStage<Void> send(Connection connection) {
        return this.connection.load(this.origin).thenCompose(self -> self.replay(connection));
    }

    @Override
    public String toString() {
        return "CachedResponse{" +
            "origin=" + origin +
            ", con=" + connection +
            '}';
    }

    /**
     * Connection that keeps response state and can reply it to other connection.
     */
    private static final class StatefulConnection implements Connection {

        /**
         * Response status.
         */
        private volatile RsStatus status;

        /**
         * Response headers.
         */
        private volatile Headers headers;

        /**
         * Response body.
         */
        private volatile byte[] body;

        @Override
        public CompletionStage<Void> accept(final RsStatus stts, final Headers hdrs,
            final Content body) {
            this.status = stts;
            this.headers = hdrs;
            return new Content.From(body).asBytesFuture()
                .thenAccept(bytes -> this.body = bytes);
        }

        @Override
        public String toString() {
            return "StatefulConnection{" +
                "status=" + status +
                ", headers=" + headers +
                ", bodySize=" + body.length +
                '}';
        }

        /**
         * Load state from response if needed.
         * @param response Response to load the state
         * @return Self future
         */
        CompletionStage<StatefulConnection> load(final Response response) {
            if (this.status == null && this.headers == null && this.body == null) {
                return response.send(this).thenApply(none -> this);
            }
            return CompletableFuture.completedFuture(this);
        }

        /**
         * Reply self state to connection.
         * @param connection Connection
         * @return Future
         */
        CompletionStage<Void> replay(final Connection connection) {
            return connection.accept(this.status, this.headers, new Content.From(this.body));
        }
    }
}
