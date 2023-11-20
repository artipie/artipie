/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import java.net.URI;
import java.net.URISyntaxException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ContentFileName}.
 *
 * @since 0.17.8
 */
final class ContentFileNameTest {

    @Test
    void shouldBeContentDispositionHeader() {
        MatcherAssert.assertThat(
            new ContentFileName("bar.txt").getKey(),
            new IsEqual<>("Content-Disposition")
        );
    }

    @Test
    void shouldHaveQuotedValue() {
        MatcherAssert.assertThat(
            new ContentFileName("foo.txt").getValue(),
            new IsEqual<>("attachment; filename=\"foo.txt\"")
        );
    }

    @Test
    void shouldTakeUriAsParameter() throws URISyntaxException {
        MatcherAssert.assertThat(
            new ContentFileName(
                new URI("https://example.com/index.html")
            ).getValue(),
            new IsEqual<>("attachment; filename=\"index.html\"")
        );
    }
}
