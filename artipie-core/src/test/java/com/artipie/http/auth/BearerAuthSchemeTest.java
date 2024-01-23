/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.Headers;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link BearerAuthScheme}.
 *
 * @since 0.17
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BearerAuthSchemeTest {

    @Test
    void shouldExtractTokenFromHeaders() {
        final String token = "12345";
        final AtomicReference<String> capture = new AtomicReference<>();
        new BearerAuthScheme(
            tkn -> {
                capture.set(tkn);
                return CompletableFuture.completedFuture(
                    Optional.of(new AuthUser("alice"))
                );
            },
            "realm=\"artipie.com\""
        ).authenticate(
            new Headers.From(new Authorization.Bearer(token)),
            "GET http://not/used HTTP/1.1"
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            capture.get(),
            new IsEqual<>(token)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"bob", "jora"})
    void shouldReturnUserInResult(final String name) {
        final AuthUser user = new AuthUser(name);
        final AuthScheme.Result result = new BearerAuthScheme(
            tkn -> CompletableFuture.completedFuture(Optional.of(user)),
            "whatever"
        ).authenticate(
            new Headers.From(new Authorization.Bearer("abc")), "GET http://any HTTP/1.1"
        ).toCompletableFuture().join();
        Assertions.assertSame(AuthScheme.AuthStatus.AUTHENTICATED, result.status());
        MatcherAssert.assertThat(result.user(), Matchers.is(user));
    }

    @Test
    void shouldReturnAnonymousUserWhenNoAuthorizationHeader() {
        final String params = "realm=\"artipie.com/auth\",param1=\"123\"";
        final AuthScheme.Result result = new BearerAuthScheme(
            tkn -> CompletableFuture.completedFuture(Optional.empty()), params
        ).authenticate(
            new Headers.From(new Header("X-Something", "some value")),
            "GET http://ignored HTTP/1.1"
        ).toCompletableFuture().join();
        Assertions.assertSame(
            AuthScheme.AuthStatus.NO_CREDENTIALS,
            result.status()
        );
        Assertions.assertTrue(
            result.user().isAnonymous(),
            "Should return anonymous user"
        );
    }

    @ParameterizedTest
    @MethodSource("badHeaders")
    void shouldNotBeAuthorizedWhenNoBearerHeader(final Headers headers) {
        final String params = "realm=\"artipie.com/auth\",param1=\"123\"";
        final AuthScheme.Result result = new BearerAuthScheme(
            tkn -> CompletableFuture.completedFuture(Optional.empty()),
            params
        ).authenticate(headers, "GET http://ignored HTTP/1.1")
            .toCompletableFuture()
            .join();
        Assertions.assertNotSame(AuthScheme.AuthStatus.AUTHENTICATED, result.status());
        MatcherAssert.assertThat(
            "Has expected challenge",
            result.challenge(),
            new IsEqual<>(String.format("Bearer %s", params))
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Headers> badHeaders() {
        return Stream.of(
            new Headers.From(),
            new Headers.From(new Header("X-Something", "some value")),
            new Headers.From(new Authorization.Basic("charlie", "qwerty"))
        );
    }
}
