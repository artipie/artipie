/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Authorization.Bearer}.
 *
 * @since 0.12
 */
public final class AuthorizationBearerTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new Authorization.Bearer("mF_9.B5f-4.1JqM").getValue(),
            new IsEqual<>("Bearer mF_9.B5f-4.1JqM")
        );
    }

    @Test
    void shouldHaveExpectedToken() {
        final String token = "123.abc";
        MatcherAssert.assertThat(
            new Authorization.Bearer(token).token(),
            new IsEqual<>(token)
        );
    }
}
