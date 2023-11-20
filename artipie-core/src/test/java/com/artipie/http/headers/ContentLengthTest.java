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
 * Test case for {@link ContentLength}.
 *
 * @since 0.10
 */
public final class ContentLengthTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new ContentLength("10").getKey(),
            new IsEqual<>("Content-Length")
        );
    }

    @Test
    void shouldExtractLongValueFromHeaders() {
        final long length = 123;
        final ContentLength header = new ContentLength(
            new Headers.From(
                new Header("Content-Type", "application/octet-stream"),
                new Header("content-length", String.valueOf(length)),
                new Header("X-Something", "Some Value")
            )
        );
        MatcherAssert.assertThat(header.longValue(), new IsEqual<>(length));
    }

    @Test
    void shouldFailToExtractLongValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ContentLength(Headers.EMPTY).longValue()
        );
    }
}
