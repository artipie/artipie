/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.api.AuthTokenRest;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.auth.Tokens;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;

/**
 * Implementation to manage JWT tokens.
 * @since 0.29
 */
public final class JwtTokens implements Tokens {

    /**
     * Jwt auth provider.
     */
    private final JWTAuth provider;

    /**
     * Ctor.
     * @param provider Jwt auth provider
     */
    public JwtTokens(final JWTAuth provider) {
        this.provider = provider;
    }

    @Override
    public TokenAuthentication auth() {
        return new JwtTokenAuth(this.provider);
    }

    @Override
    public String generate(final Authentication.User user) {
        return this.provider.generateToken(
            new JsonObject().put(AuthTokenRest.SUB, user.name())
                .put(AuthTokenRest.GROUPS, user.groups())
        );
    }
}
