/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ContentWithSize}.
 * @since 0.18
 */
final class ContentWithSizeTest {

    @Test
    void parsesHeaderValue() {
        final long length = 100L;
        MatcherAssert.assertThat(
            new ContentWithSize(Content.EMPTY, new Headers.From(new ContentLength(length))).size()
                .orElse(0L),
            new IsEqual<>(length)
        );
    }
}
