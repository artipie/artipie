/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link AuthClientSlice}.
 *
 * @since 0.3
 */
final class AuthClientSliceTest {

    @Test
    void shouldAuthenticateFirstRequestWithEmptyHeadersFirst() {
        final FakeAuthenticator fake = new FakeAuthenticator(Headers.EMPTY);
        new AuthClientSlice(
            (line, headers, body) -> StandardRs.EMPTY,
            fake
        ).response(
            new RequestLine(RqMethod.GET, "/"),
            Headers.from("X-Header", "The Value"),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.capture(0),
            new IsEqual<>(Headers.EMPTY)
        );
    }

    @Test
    void shouldAuthenticateOnceIfNotUnauthorized() {
        final AtomicReference<Headers> capture = new AtomicReference<>();
        final Header original = new Header("Original", "Value");
        final Authorization.Basic auth = new Authorization.Basic("me", "pass");
        new AuthClientSlice(
            (line, headers, body) -> {
                Headers aa = headers.copy();
                capture.set(aa);
                return StandardRs.EMPTY;
            },
            new FakeAuthenticator(Headers.from(auth))
        ).response(
            new RequestLine(RqMethod.GET, "/resource"),
            Headers.from(original),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();

        MatcherAssert.assertThat(
            capture.get(),
            Matchers.containsInAnyOrder(original, auth)
        );
    }

    @Test
    void shouldAuthenticateWithHeadersIfUnauthorized() {
        final Header rsheader = new Header("Abc", "Def");
        final FakeAuthenticator fake = new FakeAuthenticator(Headers.EMPTY, Headers.EMPTY);
        new AuthClientSlice(
            (line, headers, body) -> new RsWithHeaders(
                new RsWithStatus(RsStatus.UNAUTHORIZED),
                Headers.from(rsheader)
            ),
            fake
        ).response(
            new RequestLine(RqMethod.GET, "/foo/bar"),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.capture(1),
            Matchers.containsInAnyOrder(rsheader)
        );
    }

    @Test
    void shouldAuthenticateOnceIfUnauthorizedButAnonymous() {
        final AtomicInteger capture = new AtomicInteger();
        new AuthClientSlice(
            (line, headers, body) -> {
                capture.incrementAndGet();
                return new RsWithStatus(RsStatus.UNAUTHORIZED);
            },
            Authenticator.ANONYMOUS
        ).response(
            new RequestLine(RqMethod.GET, "/secret/resource"),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            new IsEqual<>(1)
        );
    }

    @Test
    void shouldAuthenticateTwiceIfNotUnauthorized() {
        final AtomicReference<Headers> capture = new AtomicReference<>();
        final Header original = new Header("RequestHeader", "Original Value");
        final Authorization.Basic auth = new Authorization.Basic("user", "password");
        new AuthClientSlice(
            (line, headers, body) -> {
                capture.set(headers);
                return new RsWithStatus(RsStatus.UNAUTHORIZED);
            },
            new FakeAuthenticator(Headers.EMPTY, Headers.from(auth))
        ).response(
            new RequestLine(RqMethod.GET, "/top/secret"),
            Headers.from(original),
            Content.EMPTY
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            Matchers.containsInAnyOrder(original, auth)
        );
    }

    @Test
    void shouldNotCompleteOriginSentWhenAuthSentNotComplete() {
        final AtomicReference<CompletionStage<Void>> capture = new AtomicReference<>();
        new AuthClientSlice(
            (line, headers, body) -> connection -> {
                final CompletionStage<Void> sent = StandardRs.EMPTY.send(connection);
                capture.set(sent);
                return sent;
            },
            new FakeAuthenticator(Headers.EMPTY)
        ).response(
            new RequestLine(RqMethod.GET, "/path"),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> new CompletableFuture<>()
        );
        Assertions.assertThrows(
            TimeoutException.class,
            () -> {
                final int timeout = 500;
                capture.get().toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
            }
        );
    }

    @Test
    void shouldPassRequestForBothAttempts() {
        final Headers auth = Headers.from("some", "header");
        final byte[] request = "request".getBytes();
        final AtomicReference<List<byte[]>> capture = new AtomicReference<>(new ArrayList<>(0));
        new AuthClientSlice(
            (line, headers, body) -> new AsyncResponse(
                new Content.From(body).asBytesFuture().thenApply(
                    bytes -> {
                        capture.get().add(bytes);
                        return new RsWithStatus(RsStatus.UNAUTHORIZED);
                    }
                )
            ),
            new FakeAuthenticator(auth, auth)
        ).response(
            new RequestLine(RqMethod.GET, "/api"),
            Headers.EMPTY,
            new Content.OneTime(new Content.From(request))
        ).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Body sent in first request",
            capture.get().get(0),
            new IsEqual<>(request)
        );
        MatcherAssert.assertThat(
            "Body sent in second request",
            capture.get().get(1),
            new IsEqual<>(request)
        );
    }

    /**
     * Fake authenticator providing specified results
     * and capturing `authenticate()` method arguments.
     *
     * @since 0.3
     */
    private static final class FakeAuthenticator implements Authenticator {

        /**
         * Results `authenticate()` method should return by number of invocation.
         */
        private final List<Headers> results;

        /**
         * Captured `authenticate()` method arguments by number of invocation..
         */
        private final AtomicReference<List<Headers>> captures;

        private FakeAuthenticator(final Headers... results) {
            this(Arrays.asList(results));
        }

        private FakeAuthenticator(final List<Headers> results) {
            this.results = results;
            this.captures = new AtomicReference<>(Collections.emptyList());
        }

        public Headers capture(final int index) {
            return this.captures.get().get(index);
        }

        @Override
        public CompletionStage<Headers> authenticate(final Headers headers) {
            final List<Headers> prev = this.captures.get();
            final List<Headers> updated = new ArrayList<>(prev);
            updated.add(headers);
            this.captures.set(updated);
            return CompletableFuture.completedFuture(this.results.get(prev.size()));
        }
    }
}
