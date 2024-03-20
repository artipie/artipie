/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.client.HttpServer;
import com.artipie.http.client.ProxySettings;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link JettyClientSlices}.
 */
final class JettyClientSlicesTest {

    private final HttpServer server = new HttpServer();

    @BeforeEach
    void setUp() {
        this.server.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
    }

    @Test
    void shouldProduceHttp() {
        MatcherAssert.assertThat(
            new JettyClientSlices().http("example.com"),
            new IsInstanceOf(JettyClientSlice.class)
        );
    }

    @Test
    void shouldProduceHttpWithPort() {
        final int custom = 8080;
        MatcherAssert.assertThat(
            new JettyClientSlices().http("localhost", custom),
            new IsInstanceOf(JettyClientSlice.class)
        );
    }

    @Test
    void shouldProduceHttps() {
        MatcherAssert.assertThat(
            new JettyClientSlices().http("artipie.com"),
            new IsInstanceOf(JettyClientSlice.class)
        );
    }

    @Test
    void shouldProduceHttpsWithPort() {
        final int custom = 9876;
        MatcherAssert.assertThat(
            new JettyClientSlices().http("www.artipie.com", custom),
            new IsInstanceOf(JettyClientSlice.class)
        );
    }

    @Test
    void shouldSupportProxy() throws Exception {
        final byte[] response = "response from proxy".getBytes();
        this.server.update(
            (line, headers, body) -> new RsWithBody(
                Flowable.just(ByteBuffer.wrap(response))
            )
        );
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().addProxy(
                new ProxySettings("http", "localhost", this.server.port())
            )
        );
        try {
            client.start();
            MatcherAssert.assertThat(
                client.http("artipie.com").response(
                    new RequestLine(RqMethod.GET, "/"),
                    Headers.EMPTY,
                    Content.EMPTY
                ),
                new RsHasBody(response)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    void shouldNotFollowRedirectIfDisabled() {
        final RsStatus status = RsStatus.TEMPORARY_REDIRECT;
        this.server.update(
            (line, headers, body) -> new RsWithHeaders(
                new RsWithStatus(status),
                "Location", "/other/path"
            )
        );
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setFollowRedirects(false)
        );
        try {
            client.start();
            MatcherAssert.assertThat(
                client.http("localhost", this.server.port()).response(
                    new RequestLine(RqMethod.GET, "/some/path"),
                    Headers.EMPTY,
                    Content.EMPTY
                ),
                new RsHasStatus(status)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    void shouldFollowRedirectIfEnabled() {
        this.server.update(
            (line, headers, body) -> {
                final Response result;
                if (line.toString().contains("target")) {
                    result = new RsWithStatus(RsStatus.OK);
                } else {
                    result = new RsWithHeaders(
                        new RsWithStatus(RsStatus.TEMPORARY_REDIRECT),
                        "Location", "/target"
                    );
                }
                return result;
            }
        );
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setFollowRedirects(true)
        );
        try {
            client.start();
            MatcherAssert.assertThat(
                client.http("localhost", this.server.port()).response(
                    new RequestLine(RqMethod.GET, "/some/path"),
                    Headers.EMPTY,
                    Content.EMPTY
                ),
                new RsHasStatus(RsStatus.OK)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void shouldTimeoutConnectionIfDisabled() {
        final int timeout = 1;
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setConnectTimeout(0)
        );
        try {
            client.start();
            final String nonroutable = "10.0.0.0";
            final CompletionStage<Void> received = client.http(nonroutable).response(
                new RequestLine(RqMethod.GET, "/conn-timeout"),
                Headers.EMPTY,
                Content.EMPTY
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            );
            Assertions.assertThrows(
                TimeoutException.class,
                () -> received.toCompletableFuture().get(timeout + 1, TimeUnit.SECONDS)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void shouldTimeoutConnectionIfEnabled() {
        final int timeout = 5_000;
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setConnectTimeout(timeout)
        );
        try {
            client.start();
            final String nonroutable = "10.0.0.0";
            final CompletionStage<Void> received = client.http(nonroutable).response(
                new RequestLine(RqMethod.GET, "/conn-timeout"),
                Headers.EMPTY,
                Content.EMPTY
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            );
            Assertions.assertThrows(
                ExecutionException.class,
                () -> received.toCompletableFuture().get(timeout + 1, TimeUnit.SECONDS)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    void shouldTimeoutIdleConnectionIfEnabled() throws Exception {
        final int timeout = 1_000;
        this.server.update((line, headers, body) -> connection -> new CompletableFuture<>());
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setIdleTimeout(timeout)
        );
        try {
            client.start();
            final CompletionStage<Void> received = client.http(
                "localhost",
                this.server.port()
            ).response(
                new RequestLine(RqMethod.GET, "/idle-timeout"),
                Headers.EMPTY,
                Content.EMPTY
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            );
            Assertions.assertThrows(
                ExecutionException.class,
                () -> received.toCompletableFuture().get(timeout + 1, TimeUnit.SECONDS)
            );
        } finally {
            client.stop();
        }
    }

    @Test
    void shouldNotTimeoutIdleConnectionIfDisabled() throws Exception {
        this.server.update((line, headers, body) -> connection -> new CompletableFuture<>());
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setIdleTimeout(0)
        );
        try {
            client.start();
            final CompletionStage<Void> received = client.http(
                "localhost",
                this.server.port()
            ).response(
                new RequestLine(RqMethod.GET, "/idle-timeout"),
                Headers.EMPTY,
                Content.EMPTY
            ).send(
                (status, headers, body) -> CompletableFuture.allOf()
            );
            Assertions.assertThrows(
                TimeoutException.class,
                () -> received.toCompletableFuture().get(1, TimeUnit.SECONDS)
            );
        } finally {
            client.stop();
        }
    }

    @Disabled("https://github.com/artipie/artipie/issues/1413")
    @ParameterizedTest
    @CsvSource({
        "expired.badssl.com",
        "self-signed.badssl.com",
        "untrusted-root.badssl.com"
    })
    void shouldTrustAllCertificates(final String url) throws Exception {
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setTrustAll(true)
        );
        try {
            client.start();
            MatcherAssert.assertThat(
                client.https(url).response(
                    new RequestLine(RqMethod.GET, "/"),
                    Headers.EMPTY,
                    Content.EMPTY
                ),
                new RsHasStatus(RsStatus.OK)
            );
        } finally {
            client.stop();
        }
    }

    @Disabled("https://github.com/artipie/artipie/issues/1413")
    @ParameterizedTest
    @CsvSource({
        "expired.badssl.com",
        "self-signed.badssl.com",
        "untrusted-root.badssl.com"
    })
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    void shouldRejectBadCertificates(final String url) throws Exception {
        final JettyClientSlices client = new JettyClientSlices(
            new HttpClientSettings().setTrustAll(false)
        );
        try {
            client.start();
            final Response response = client.https(url).response(
                new RequestLine(RqMethod.GET, "/"),
                Headers.EMPTY,
                Content.EMPTY
            );
            final Exception exception = Assertions.assertThrows(
                CompletionException.class,
                () -> response
                    .send(
                        (status, headers, publisher) ->
                            CompletableFuture.allOf()
                    )
                    .toCompletableFuture().join()
            );
            MatcherAssert.assertThat(
                exception,
                Matchers.hasProperty(
                    "cause",
                    Matchers.isA(SSLException.class)
                )
            );
        } finally {
            client.stop();
        }
    }
}
