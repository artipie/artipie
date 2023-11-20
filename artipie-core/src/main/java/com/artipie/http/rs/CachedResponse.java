/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.reactivestreams.Publisher;

/**
 * Response that caches origin response once it first sent and can replay it many times.
 * <p>It can be useful when testing one response against multiple matchers, and response
 * from slice should be called only once.</p>
 * @since 0.17
 */
public final class CachedResponse implements Response {

    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Stateful connection.
     */
    private final StatefulConnection con;

    /**
     * Wraps response with stateful connection.
     * @param origin Origin response
     */
    public CachedResponse(final Response origin) {
        this.origin = origin;
        this.con = new StatefulConnection();
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        return this.con.load(this.origin).thenCompose(self -> self.replay(connection));
    }

    @Override
    public String toString() {
        return String.format(
            "(%s: state=%s)",
            this.getClass().getSimpleName(),
            this.con.toString()
        );
    }

    /**
     * Connection that keeps response state and can reply it to other connection.
     * @since 0.16
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
            final Publisher<ByteBuffer> bdy) {
            this.status = stts;
            this.headers = hdrs;
            return new PublisherAs(bdy).bytes().thenAccept(
                bytes -> this.body = bytes
            );
        }

        @Override
        public String toString() {
            return String.format(
                "(%s: status=%s, headers=[%s], body=%s)",
                this.getClass().getSimpleName(),
                this.status,
                StreamSupport.stream(this.headers.spliterator(), false)
                    .map(
                        header -> String.format(
                            "\"%s\": \"%s\"",
                            header.getKey(),
                            header.getValue()
                        )
                    ).collect(Collectors.joining(", ")),
                Arrays.toString(this.body)
            );
        }

        /**
         * Load state from response if needed.
         * @param response Response to load the state
         * @return Self future
         */
        CompletionStage<StatefulConnection> load(final Response response) {
            final CompletionStage<StatefulConnection> self;
            if (this.status == null && this.headers == null && this.body == null) {
                self = response.send(this).thenApply(none -> this);
            } else {
                self = CompletableFuture.completedFuture(this);
            }
            return self;
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
