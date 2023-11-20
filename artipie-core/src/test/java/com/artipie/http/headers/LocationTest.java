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
 * Test case for {@link Location}.
 *
 * @since 0.11
 */
public final class LocationTest {

    @Test
    void shouldHaveExpectedName() {
        MatcherAssert.assertThat(
            new Location("http://artipie.com/").getKey(),
            new IsEqual<>("Location")
        );
    }

    @Test
    void shouldHaveExpectedValue() {
        final String value = "http://artipie.com/something";
        MatcherAssert.assertThat(
            new Location(value).getValue(),
            new IsEqual<>(value)
        );
    }

    @Test
    void shouldExtractValueFromHeaders() {
        final String value = "http://artipie.com/resource";
        final Location header = new Location(
            new Headers.From(
                new Header("Content-Length", "11"),
                new Header("location", value),
                new Header("X-Something", "Some Value")
            )
        );
        MatcherAssert.assertThat(header.getValue(), new IsEqual<>(value));
    }

    @Test
    void shouldFailToExtractValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Location(Headers.EMPTY).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueWhenNoLocationHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Location(
                new Headers.From("Content-Type", "text/plain")
            ).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueFromMultipleHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new Location(
                new Headers.From(
                    new Location("http://artipie.com/1"),
                    new Location("http://artipie.com/2")
                )
            ).getValue()
        );
    }
}
