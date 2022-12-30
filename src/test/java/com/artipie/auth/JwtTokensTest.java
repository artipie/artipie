/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link JwtTokens}.
 * @since 0.29
 */
class JwtTokensTest {

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
    void returnsAuth() {
        MatcherAssert.assertThat(
            new JwtTokens(this.provider).auth(),
            new IsInstanceOf(JwtTokenAuth.class)
        );
    }

    @Test
    void generatesToken() {
        MatcherAssert.assertThat(
            new JwtTokens(this.provider).generate(new Authentication.User("Oleg")),
            new IsNot<>(Matchers.emptyString())
        );
    }

}
