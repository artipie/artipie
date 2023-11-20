/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Authorization.Token}.
 *
 * @since 0.23
 */
public final class AuthorizationTokenTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new Authorization.Token("abc123").getValue(),
            new IsEqual<>("token abc123")
        );
    }

    @Test
    void shouldHaveExpectedToken() {
        final String token = "098.xyz";
        MatcherAssert.assertThat(
            new Authorization.Token(token).token(),
            new IsEqual<>(token)
        );
    }
}
