/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.util.Collections;
import java.util.List;
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
        final List<String> groups = List.of("admin");
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(new JsonObject().put("sub", name).put("groups", groups))
            ).toCompletableFuture().join().get(),
            new IsEqual<>(new Authentication.User(name, groups))
        );
    }

    @Test
    void returnsUserWhenGroupsAreEmpty() {
        final String name = "John";
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(
                    new JsonObject().put("sub", name).put("groups", Collections.emptyList())
                )
            ).toCompletableFuture().join().get(),
            new IsEqual<>(new Authentication.User(name))
        );
    }

    @Test
    void returnsEmptyWhenSubIsNotPresent() {
        MatcherAssert.assertThat(
            new JwtTokenAuth(this.provider).user(
                this.provider.generateToken(new JsonObject().put("groups", List.of("reader")))
            ).toCompletableFuture().join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsEmptyWhenGroupsAreNotPresent() {
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
