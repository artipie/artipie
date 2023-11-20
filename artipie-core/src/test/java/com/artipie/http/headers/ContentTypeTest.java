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
 * Test case for {@link ContentType}.
 *
 * @since 0.11
 */
public final class ContentTypeTest {

    @Test
    void shouldHaveExpectedName() {
        MatcherAssert.assertThat(
            new ContentType("10").getKey(),
            new IsEqual<>("Content-Type")
        );
    }

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new ContentType("10").getValue(),
            new IsEqual<>("10")
        );
    }

    @Test
    void shouldExtractValueFromHeaders() {
        final String value = "application/octet-stream";
        final ContentType header = new ContentType(
            new Headers.From(
                new Header("Content-Length", "11"),
                new Header("content-type", value),
                new Header("X-Something", "Some Value")
            )
        );
        MatcherAssert.assertThat(header.getValue(), new IsEqual<>(value));
    }

    @Test
    void shouldFailToExtractValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ContentType(Headers.EMPTY).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueWhenNoContentTypeHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ContentType(
                new Headers.From("Location", "http://artipie.com")
            ).getValue()
        );
    }

    @Test
    void shouldFailToExtractValueFromMultipleHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ContentType(
                new Headers.From(
                    new ContentType("application/json"),
                    new ContentType("text/plain")
                )
            ).getValue()
        );
    }
}
