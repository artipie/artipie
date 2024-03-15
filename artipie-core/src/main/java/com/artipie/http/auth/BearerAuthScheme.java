/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Bearer authentication method.
 *
 * @since 0.17
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class BearerAuthScheme implements AuthScheme {

    /**
     * Bearer authentication prefix.
     */
    public static final String NAME = "Bearer";

    /**
     * Authentication.
     */
    private final TokenAuthentication auth;

    /**
     * Challenge parameters.
     */
    private final String params;

    /**
     * Ctor.
     *
     * @param auth Authentication.
     * @param params Challenge parameters.
     */
    public BearerAuthScheme(final TokenAuthentication auth, final String params) {
        this.auth = auth;
        this.params = params;
    }

    @Override
    public CompletionStage<Result> authenticate(final Iterable<Map.Entry<String, String>> headers,
        final RequestLine line) {
        return new RqHeaders(headers, Authorization.NAME)
            .stream()
            .findFirst()
            .map(
                header -> this.user(header)
                    .thenApply(user -> AuthScheme.result(user, this.challenge()))
            ).orElseGet(
                () -> CompletableFuture.completedFuture(
                    AuthScheme.result(AuthUser.ANONYMOUS, this.challenge())
                )
            );
    }

    /**
     * Obtains user from authorization header.
     *
     * @param header Authorization header's value
     * @return User, empty if not authenticated
     */
    private CompletionStage<Optional<AuthUser>> user(final String header) {
        final Authorization atz = new Authorization(header);
        if (BearerAuthScheme.NAME.equals(atz.scheme())) {
            return this.auth.user(
                new Authorization.Bearer(atz.credentials()).token()
            );
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Challenge for client to be provided as WWW-Authenticate header value.
     *
     * @return Challenge string.
     */
    private String challenge() {
        return String.format("%s %s", BearerAuthScheme.NAME, this.params);
    }
}
