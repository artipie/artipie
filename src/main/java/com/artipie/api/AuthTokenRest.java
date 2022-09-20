/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import com.artipie.settings.cache.AuthCache;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Generate JWT token endpoint.
 * @since 0.2
 */
public final class AuthTokenRest extends BaseRest {

    /**
     * JWT auth provider.
     */
    private final JWTAuth provider;

    /**
     * Artipie auth cache.
     */
    private final AuthCache cache;

    /**
     * Artipie authentication.
     */
    private final Authentication auth;

    /**
     * Ctor.
     *
     * @param provider Vertx JWT auth
     * @param cache Artipie auth cache
     * @param auth Artipie authentication
     */
    public AuthTokenRest(final JWTAuth provider, final AuthCache cache, final Authentication auth) {
        this.provider = provider;
        this.cache = cache;
        this.auth = auth;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("getJwtToken")
            .handler(this::getJwtToken)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    /**
     * Validate user and get jwt token.
     * @param routing Request context
     */
    private void getJwtToken(final RoutingContext routing) {
        final JsonObject body = routing.body().asJsonObject();
        final Optional<Authentication.User> user = this.cache.user(
            body.getString("name"), body.getString("pass"), this.auth
        );
        if (user.isPresent()) {
            routing.response().setStatusCode(HttpStatus.OK_200).end(
                new JsonObject().put(
                    "token",
                    this.provider.generateToken(
                        new JsonObject().put("sub", user.get().name())
                    )
                ).encode()
            );
        } else {
            routing.response().setStatusCode(HttpStatus.UNAUTHORIZED_401).send();
        }
    }

}
