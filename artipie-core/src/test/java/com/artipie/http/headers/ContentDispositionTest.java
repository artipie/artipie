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
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Test case for {@link ContentDisposition}.
 *
 * @since 0.17.8
 */
public final class ContentDispositionTest {

    @Test
    void shouldHaveExpectedValue() {
        MatcherAssert.assertThat(
            new ContentDisposition("").getKey(),
            new IsEqual<>("Content-Disposition")
        );
    }

    @Test
    void shouldExtractFileName() {
        MatcherAssert.assertThat(
            new ContentDisposition(
                new Headers.From(
                    new Header("Content-Type", "application/octet-stream"),
                    new Header("content-disposition", "attachment; filename=\"filename.jpg\"")
                )
            ).fileName(),
            new IsEqual<>("filename.jpg")
        );
    }

    @Test
    void shouldFailToExtractLongValueFromEmptyHeaders() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ContentDisposition(Headers.EMPTY).fileName()
        );
    }

    @Test
    void shouldBeInline() {
        MatcherAssert.assertThat(
            new ContentDisposition("inline").isInline(),
            new IsTrue()
        );
    }

    @Test
    void shouldBeAttachment() {
        MatcherAssert.assertThat(
            new ContentDisposition("attachment; name=\"input\"").isAttachment(),
            new IsTrue()
        );
    }

    @Test
    void parsesNameDirective() {
        MatcherAssert.assertThat(
            new ContentDisposition("attachment; name=\"field\"").fieldName(),
            new IsEqual<>("field")
        );
    }

    @Test
    void parsesFilenameDirective() {
        MatcherAssert.assertThat(
            new ContentDisposition("attachment; filename=\"foo.jpg\"").fileName(),
            new IsEqual<>("foo.jpg")
        );
    }

    @Test
    void readNameWithColons() {
        MatcherAssert.assertThat(
            new ContentDisposition("form-data; name=\":action\"").fieldName(),
            new IsEqual<>(":action")
        );
    }
}
