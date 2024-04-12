/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.api.AuthTokenRest;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Token authentication with Vert.x {@link io.vertx.ext.auth.jwt.JWTAuth} under the hood.
 */
public final class JwtTokenAuth implements TokenAuthentication {

    /**
     * Jwt auth provider.
     */
    private final JWTAuth provider;

    /**
     * @param provider Jwt auth provider
     */
    public JwtTokenAuth(JWTAuth provider) {
        this.provider = provider;
    }

    @Override
    public CompletionStage<Optional<AuthUser>> user(String token) {
        return this.provider
            .authenticate(new TokenCredentials(token))
            .map(
                user -> {
                    Optional<AuthUser> res = Optional.empty();
                    if (user.principal().containsKey(AuthTokenRest.SUB)
                        && user.containsKey(AuthTokenRest.CONTEXT)) {
                        res = Optional.of(
                            new AuthUser(
                                user.principal().getString(AuthTokenRest.SUB),
                                user.principal().getString(AuthTokenRest.CONTEXT)
                            )
                        );
                    }
                    return res;
                }
            ).otherwise(Optional.empty())
            .toCompletionStage();
    }
}
