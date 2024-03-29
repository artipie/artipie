/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.client.HttpServer;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tests checking for leaks in {@link JettyClientSlice}.
 */
final class JettyClientSliceLeakTest {

    /**
     * HTTP server used in tests.
     */
    private final HttpServer server = new HttpServer();

    /**
     * HTTP client used in tests.
     */
    private final HttpClient client = new HttpClient();

    /**
     * HTTP client sliced being tested.
     */
    private JettyClientSlice slice;

    @BeforeEach
    void setUp() throws Exception {
        this.server.update(
            (line, headers, body) -> CompletableFuture.completedFuture(ResponseBuilder.ok().textBody("data").build())
        );
        final int port = this.server.start();
        this.client.start();
        this.slice = new JettyClientSlice(this.client, false, "localhost", port);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.stop();
        this.client.stop();
    }

    @Test
    void shouldNotLeakConnectionsIfBodyNotRead() throws Exception {
        final int total = 1025;
        for (int count = 0; count < total; count += 1) {
            this.slice.response(
                new RequestLine(RqMethod.GET, "/"),
                Headers.EMPTY,
                Content.EMPTY
            ).get(1, TimeUnit.SECONDS);
        }
    }
}
