/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLine;

import javax.json.Json;
import java.util.concurrent.CompletableFuture;

/**
 * Slice for token authorization.
 */
final class GenerateTokenSlice implements Slice {

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Tokens.
     */
    private final Tokens tokens;

    /**
     * @param auth Authentication
     * @param tokens Tokens
     */
    GenerateTokenSlice(final Authentication auth, final Tokens tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return new BasicAuthScheme(this.auth).authenticate(headers)
            .toCompletableFuture()
            .thenApply(
                result -> {
                    if (result.status() == AuthScheme.AuthStatus.FAILED) {
                        return ResponseBuilder.unauthorized()
                            .header(new WwwAuthenticate(result.challenge()))
                            .build();
                    }
                    return ResponseBuilder.ok()
                        .jsonBody(
                            Json.createObjectBuilder()
                                .add("token", this.tokens.generate(result.user()))
                                .build()
                        )
                        .build();
                }
        );
    }
}
