/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.api.AuthTokenRest;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

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
    public CompletionStage<Optional<Authentication.User>> user(final String token) {
        return this.provider.authenticate(new JsonObject().put("token", token)).map(
            user -> {
                Optional<Authentication.User> res = Optional.empty();
                if (user.principal().containsKey(AuthTokenRest.SUB)
                    && user.containsKey(AuthTokenRest.GROUPS)) {
                    res = Optional.of(
                        new Authentication.User(
                            user.principal().getString(AuthTokenRest.SUB),
                            user.principal().getJsonArray(AuthTokenRest.GROUPS).stream()
                                .map(Object::toString).collect(Collectors.toList())
                        )
                    );
                }
                return res;
            }
        ).otherwise(Optional.empty()).toCompletionStage();
    }
}
