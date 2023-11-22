/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http.auth;

import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLineFrom;
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
    public CompletionStage<Result> authenticate(final Iterable<Map.Entry<String, String>> headers,
        final String line) {
        return this.user(headers, line).thenApply(
            user -> user.<Result>map(Success::new).orElse(new Failure())
        );
    }

    /**
     * Obtains user from authentication header or from request line.
     *
     * @param headers Headers
     * @param line Request line
     * @return User, empty if not authenticated
     */
    private CompletionStage<Optional<AuthUser>> user(
        final Iterable<Map.Entry<String, String>> headers, final String line
    ) {
        return new RqHeaders(headers, Authorization.NAME).stream()
            .findFirst()
            .map(Authorization::new)
            .filter(hdr -> hdr.scheme().equals(TokenAuthScheme.NAME))
            .map(hdr -> new Authorization.Token(hdr.credentials()).token())
            .map(this.auth::user)
            .orElseGet(
                () -> {
                    final Matcher mtchr = TokenAuthScheme.PTRN.matcher(
                        new RequestLineFrom(line).uri().toString()
                    );
                    CompletionStage<Optional<AuthUser>> res =
                        CompletableFuture.completedFuture(Optional.empty());
                    if (mtchr.matches()) {
                        res = this.auth.user(mtchr.group(1));
                    }
                    return res;
                }
            );
    }

    /**
     * Successful result with authenticated user.
     *
     * @since 0.5
     */
    private static class Success implements Result {

        /**
         * Authenticated user.
         */
        private final AuthUser usr;

        /**
         * Ctor.
         *
         * @param user Authenticated user.
         */
        Success(final AuthUser user) {
            this.usr = user;
        }

        @Override
        public Optional<AuthUser> user() {
            return Optional.of(this.usr);
        }

        @Override
        public String challenge() {
            return TokenAuthScheme.NAME;
        }
    }

    /**
     * Failed result without authenticated user.
     *
     * @since 0.5
     */
    private static class Failure implements Result {

        @Override
        public Optional<AuthUser> user() {
            return Optional.empty();
        }

        @Override
        public String challenge() {
            return TokenAuthScheme.NAME;
        }
    }
}
