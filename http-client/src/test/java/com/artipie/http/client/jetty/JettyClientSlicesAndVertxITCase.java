/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.cactoos.text.TextOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link JettyClientSlices} and vertx.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class JettyClientSlicesAndVertxITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Clients.
     */
    private final JettyClientSlices clients = new JettyClientSlices();

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    @BeforeEach
    void setUp() throws Exception {
        this.clients.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.clients.stop();
        if (this.server != null) {
            this.server.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getsSomeContent(final boolean anonymous) throws IOException {
        final int port = this.startServer(anonymous);
        final HttpURLConnection con = (HttpURLConnection)
            URI.create(String.format("http://localhost:%s", port)).toURL().openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        MatcherAssert.assertThat(
            "Response body is some html",
            new TextOf(con.getInputStream()).toString(),
            Matchers.startsWith("<!DOCTYPE html>")
        );
        con.disconnect();
    }

    private int startServer(final boolean anonymous) {
        this.server = new VertxSliceServer(
            JettyClientSlicesAndVertxITCase.VERTX,
            new LoggingSlice(new ProxySlice(this.clients, anonymous))
        );
        return this.server.start();
    }

    /**
     * Test proxy slice.
     * @since 0.3
     */
    static final class ProxySlice implements Slice {

        /**
         * Client.
         */
        private final ClientSlices client;

        /**
         * Anonymous flag.
         */
        private final boolean anonymous;

        /**
         * Ctor.
         * @param client Http client
         * @param anonymous Anonymous flag
         */
        ProxySlice(final ClientSlices client, final boolean anonymous) {
            this.client = client;
            this.anonymous = anonymous;
        }

        @Override
        public Response response(
            final String line,
            final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> pub
        ) {
            final CompletableFuture<Response> promise = new CompletableFuture<>();
            final Slice origin = this.client.https("blog.artipie.com");
            final Slice slice;
            if (this.anonymous) {
                slice = origin;
            } else {
                slice = new AuthClientSlice(origin, Authenticator.ANONYMOUS);
            }
            slice.response(
                new RequestLine(
                    RqMethod.GET, "/"
                ).toString(),
                Headers.EMPTY,
                Content.EMPTY
            ).send(
                (status, rsheaders, body) -> {
                    final CompletableFuture<Void> terminated = new CompletableFuture<>();
                    final Flowable<ByteBuffer> termbody = Flowable.fromPublisher(body)
                        .doOnError(terminated::completeExceptionally)
                        .doOnTerminate(() -> terminated.complete(null));
                    promise.complete(new RsFull(status, rsheaders, termbody));
                    return terminated;
                }
            );
            return new AsyncResponse(promise);
        }
    }
}
