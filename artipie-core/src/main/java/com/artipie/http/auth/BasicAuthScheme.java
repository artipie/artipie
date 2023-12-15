/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Basic authentication method.
 *
 * @since 0.17
 * @checkstyle ReturnCountCheck (500 lines)
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class BasicAuthScheme implements AuthScheme {

    /**
     * Basic authentication prefix.
     */
    public static final String NAME = "Basic";

    /**
     * Basic authentication challenge.
     */
    private static final String CHALLENGE =
        String.format("%s realm=\"artipie\"", BasicAuthScheme.NAME);

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Ctor.
     * @param auth Authentication.
     */
    public BasicAuthScheme(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public CompletionStage<Result> authenticate(
        final Iterable<Map.Entry<String, String>> headers, final String line
    ) {
        final AuthScheme.Result result = new RqHeaders(headers, Authorization.NAME)
            .stream()
            .findFirst()
            .map(s -> AuthScheme.result(this.user(s), BasicAuthScheme.CHALLENGE))
            .orElseGet(() -> AuthScheme.result(AuthUser.ANONYMOUS, BasicAuthScheme.CHALLENGE));
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Obtains user from authorization header.
     *
     * @param header Authorization header's value
     * @return User if authorised
     */
    private Optional<AuthUser> user(final String header) {
        final Authorization atz = new Authorization(header);
        if (atz.scheme().equals(BasicAuthScheme.NAME)) {
            final Authorization.Basic basic = new Authorization.Basic(atz.credentials());
            return this.auth.user(basic.username(), basic.password());
        }
        return Optional.empty();
    }
}
