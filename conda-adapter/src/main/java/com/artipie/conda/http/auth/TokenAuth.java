/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http.auth;

import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Simple in memory implementation of {@link TokenAuthentication}.
 */
public final class TokenAuth implements TokenAuthentication {

    /**
     * Tokens.
     */
    private final TokenAuthentication tokens;

    /**
     * Ctor.
     * @param tokens Tokens and users
     */
    public TokenAuth(final TokenAuthentication tokens) {
        this.tokens = tokens;
    }

    @Override
    public CompletionStage<Optional<AuthUser>> user(final String token) {
        return this.tokens.user(token);
    }
}
