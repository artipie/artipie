/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.AuthUser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link JwtTokenAuth}.
 * @since 0.29
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JwtTokenAuthTest {

    /**
     * Test JWT provider.
     */
    private JWTAuth provider;

    @BeforeEach
    void init() {
        this.provider = JWTAuth.create(
            Vertx.vertx(),
            new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("some secret")
            )
        );
    }

    @Test
    void returnsUser() {
        final String name = "Alice";
        final String cntx = "artipie";
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(new JsonObject().put("sub", name).put("context", cntx))
            ).toCompletableFuture().join().get(),
            new IsEqual<>(new AuthUser(name, cntx))
        );
    }

    @Test
    void returnsEmptyWhenSubIsNotPresent() {
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(new JsonObject().put("context", "any"))
            ).toCompletableFuture().join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsEmptyWhenContextIsNotPresent() {
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(new JsonObject().put("sub", "Alex"))
            ).toCompletableFuture().join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsEmptyWhenTokenIsNotValid() {
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user("not valid token")
                .toCompletableFuture().join().isEmpty(),
            new IsEqual<>(true)
        );
    }

}
