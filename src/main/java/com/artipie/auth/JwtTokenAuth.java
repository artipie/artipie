/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.api.AuthTokenRest;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Token authentication with Vert.x {@link io.vertx.ext.auth.jwt.JWTAuth} under the hood.
 * @since 0.29
 */
public final class JwtTokenAuth implements TokenAuthentication {

    /**
     * Jwt auth provider.
     */
    private final JWTAuth provider;

    /**
     * Ctor.
     * @param provider Jwt auth provider
     */
    public JwtTokenAuth(final JWTAuth provider) {
        this.provider = provider;
    }

    @Override
    public CompletionStage<Optional<AuthUser>> user(final String token) {
        return this.provider.authenticate(new JsonObject().put("token", token)).map(
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
        ).otherwise(Optional.empty()).toCompletionStage();
    }
}
