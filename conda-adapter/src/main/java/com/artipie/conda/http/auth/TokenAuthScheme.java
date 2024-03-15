/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http.auth;

import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conda token auth scheme.
 * @since 0.5
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class TokenAuthScheme implements AuthScheme {

    /**
     * Token authentication prefix.
     */
    public static final String NAME = "token";

    /**
     * Request line pattern.
     */
    private static final Pattern PTRN = Pattern.compile("/t/([^/]*)/.*");

    /**
     * Token authentication.
     */
    private final TokenAuthentication auth;

    /**
     * Ctor.
     * @param auth Token authentication
     */
    public TokenAuthScheme(final TokenAuthentication auth) {
        this.auth = auth;
    }

    @Override
    public CompletionStage<Result> authenticate(
        final Iterable<Map.Entry<String, String>> headers,
        final RequestLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Request line cannot be null");
        }
        final CompletionStage<Optional<AuthUser>> fut = new RqHeaders(headers, Authorization.NAME)
            .stream()
            .findFirst()
            .map(this::user)
            .orElseGet(
                () -> {
                    final Matcher mtchr = TokenAuthScheme.PTRN.matcher(line.uri().toString());
                    return mtchr.matches()
                        ? this.auth.user(mtchr.group(1))
                        : CompletableFuture.completedFuture(Optional.of(AuthUser.ANONYMOUS));
                });
        return fut.thenApply(user -> AuthScheme.result(user, TokenAuthScheme.NAME));
    }

    /**
     * Obtains user from authorization header or from request line.
     *
     * @param header Authorization header's value
     * @return User, empty if not authenticated
     */
    private CompletionStage<Optional<AuthUser>> user(final String header) {
        final Authorization atz = new Authorization(header);
        if (TokenAuthScheme.NAME.equals(atz.scheme())) {
            return this.auth.user(
                new Authorization.Token(atz.credentials()).token()
            );
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
