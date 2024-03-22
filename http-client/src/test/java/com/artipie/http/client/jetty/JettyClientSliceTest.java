/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.HttpServer;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.BaseResponse;
import com.artipie.http.rs.RsStatus;
import io.vertx.core.http.HttpServerOptions;
import org.eclipse.jetty.client.HttpClient;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link JettyClientSlice} with HTTP server.
 */
class JettyClientSliceTest {

    /**
     * Test server.
     */
    private final HttpServer server = new HttpServer();

    /**
     * HTTP client used in tests.
     */
    private HttpClient client;

    /**
     * HTTP client sliced being tested.
     */
    private JettyClientSlice slice;

    @BeforeEach
    void setUp() throws Exception {
        final int port = this.server.start(this.newHttpServerOptions());
        this.client = this.newHttpClient();
        this.client.start();
        this.slice = new JettyClientSlice(
            this.client,
            this.client.getSslContextFactory().isTrustAll(),
            "localhost",
            port
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.stop();
        this.client.stop();
    }

    HttpClient newHttpClient() {
        return new HttpClient();
    }

    HttpServerOptions newHttpServerOptions() {
        return new HttpServerOptions().setPort(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "PUT /",
        "GET /index.html",
        "POST /path?param1=value&param2=something",
        "HEAD /my%20path?param=some%20value"
    })
    void shouldSendRequestLine(final String line) {
        final AtomicReference<RequestLine> actual = new AtomicReference<>();
        this.server.update(
            (rqline, rqheaders, rqbody) -> {
                actual.set(rqline);
                return BaseResponse.ok();
            }
        );
        this.slice.response(
            RequestLine.from(String.format("%s HTTP/1.1", line)),
            Headers.EMPTY,
            Content.EMPTY
        ).send((status, headers, body) -> CompletableFuture.allOf()).toCompletableFuture().join();
        MatcherAssert.assertThat(
            actual.get().toString(),
            new StringStartsWith(String.format("%s HTTP", line))
        );
    }

    @Test
    void shouldSendHeaders() {
        final AtomicReference<Headers> actual = new AtomicReference<>();
        this.server.update(
            (line, headers, content) -> {
                System.out.println("MY_DEBUG " + headers);
                actual.set(headers);
                return BaseResponse.ok();
            }
        );
        this.slice.response(
            new RequestLine(RqMethod.GET, "/something"),
            Headers.from(
                new Header("My-Header", "MyValue"),
                new Header("Another-Header", "AnotherValue")
            ),
            Content.EMPTY
        ).send((status, headers, body) -> CompletableFuture.allOf()).toCompletableFuture().join();
        Assertions.assertEquals("MyValue", actual.get().values("My-Header").getFirst());
        Assertions.assertEquals("AnotherValue", actual.get().values("Another-Header").getFirst());
    }

    @Test
    void shouldSendBody() {
        final byte[] content = "some content".getBytes();
        final AtomicReference<byte[]> actual = new AtomicReference<>();
        this.server.update(
            (rqline, rqheaders, rqbody) -> new AsyncResponse(
                new Content.From(rqbody).asBytesFuture().thenApply(
                    bytes -> {
                        actual.set(bytes);
                        return BaseResponse.ok();
                    }
                )
            )
        );
        this.slice.response(
            new RequestLine(RqMethod.PUT, "/package"),
            Headers.EMPTY,
            new Content.From(content)
        ).send((status, headers, body) -> CompletableFuture.allOf()).toCompletableFuture().join();
        MatcherAssert.assertThat(
            actual.get(),
            new IsEqual<>(content)
        );
    }

    @Test
    void shouldReceiveStatus() {
        this.server.update((rqline, rqheaders, rqbody) -> BaseResponse.notFound());
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.GET, "/a/b/c"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldReceiveHeaders() {
        Headers headers = Headers.from(
            new Header("Content-Type", "text/plain"),
            new Header("WWW-Authenticate", "Basic")
        );
        this.server.update(
            (rqline, rqheaders, rqbody) -> BaseResponse.ok().headers(headers)
        );
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.HEAD, "/content"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasHeaders(headers)
        );
    }

    @Test
    void shouldReceiveBody() {
        final byte[] data = "data".getBytes();
        this.server.update(
            (rqline, rqheaders, rqbody) -> BaseResponse.ok().body(data)
        );
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.PATCH, "/file.txt"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasBody(data)
        );
    }
}
