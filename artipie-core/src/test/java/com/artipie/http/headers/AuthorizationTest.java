/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Authorization}.
 *
 * @since 0.12
 */
public final class AuthorizationTest {

    @Test
    void shouldHaveExpectedName() {
        MatcherAssert.assertThat(
            new Authorization("Basic abc").getKey(),
            new IsEqual<>("Authorization")
        );
    }

    @Test
    void shouldHaveExpectedValue() {
        final String value = "Basic 123";
        MatcherAssert.assertThat(
            new Authorization(value).getValue(),
            new IsEqual<>(value)
        );
    }

    @Test
    void shouldExtractValueFromHeaders() {
        final String value = "Bearer abc";
        final Authorization header = new Authorization(
            new Headers.From(
                new Header("Content-Length", "11"),
                new Header("authorization", value),
                new Header("X-Something", "Some Value")
            )
        );
        MatcherAssert.assertThat(header.getValue(), new IsEqual<>(value));
    }

    @Test
    void shouldHaveExpectedScheme() {
        MatcherAssert.assertThat(
            new Authorization("Digest abc===").scheme(),
            new IsEqual<>("Digest")
        );
    }

    @Test
    void shouldHaveExpectedCredentials() {
        MatcherAssert.assertThat(
            new Authorization("Bearer 123.abc").credentials(),
            new IsEqual<>("123.abc")
        );
    }

    @Test
    void shouldFailToParseSchemeWhenInvalidFormat() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Authorization("some_text").scheme()
        );
    }

    @Test
    void shouldFailToParseCredentialsWhenInvalidFormat() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Authorization("whatever").credentials()
        );
    }

    @Test
    void shouldFailToExtractValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Authorization(Headers.EMPTY).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueWhenNoAuthorizationHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Authorization(
                new Headers.From("Content-Type", "text/plain")
            ).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueFromMultipleHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Authorization(
                new Headers.From(
                    new Authorization("Bearer one"),
                    new Authorization("Bearer two")
                )
            ).getValue()
        );
    }
}
