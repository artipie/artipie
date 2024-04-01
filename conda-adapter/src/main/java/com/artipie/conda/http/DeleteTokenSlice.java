/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.conda.http.auth.TokenAuthScheme;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Delete token slice.
 * <a href="https://api.anaconda.org/docs#/authentication/delete_authentications">Documentation</a>.
 * This slice checks if the token is valid and returns 201 if yes. Token itself is not removed
 * from the Artipie.
 */
final class DeleteTokenSlice implements Slice {

    /**
     * Auth tokens.
     */
    private final Tokens tokens;

    /**
     * Ctor.
     * @param tokens Auth tokens
     */
    DeleteTokenSlice(final Tokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public CompletableFuture<Response> response(final RequestLine line,
                                                final Headers headers, final Content body) {

        Optional<String> opt = new RqHeaders(headers, Authorization.NAME)
            .stream().findFirst().map(Authorization::new)
            .map(auth -> new Authorization.Token(auth.credentials()).token());
        if (opt.isPresent()) {
            String token = opt.get();
            return this.tokens.auth()
                .user(token)
                .toCompletableFuture()
                .thenApply(
                    user -> user.isPresent()
                        ? ResponseBuilder.created().build()
                        : ResponseBuilder.badRequest().build()
                );
        }
        return ResponseBuilder.unauthorized()
            .header(new WwwAuthenticate(TokenAuthScheme.NAME))
            .completedFuture();
    }
}
