/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.headers.Header;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Headers.From}.
 *
 * @since 0.11
 */
class HeadersFromTest {

    @Test
    public void shouldConcatWithHeader() {
        final Header header = new Header("h1", "v1");
        final String name = "h2";
        final String value = "v2";
        MatcherAssert.assertThat(
            new Headers.From(new Headers.From(header), name, value),
            Matchers.contains(header, new Header(name, value))
        );
    }

    @Test
    public void shouldConcatWithHeaders() {
        final Header origin = new Header("hh1", "vv1");
        final Header one = new Header("hh2", "vv2");
        final Header two = new Header("hh3", "vv3");
        MatcherAssert.assertThat(
            new Headers.From(new Headers.From(origin), one, two),
            Matchers.contains(origin, one, two)
        );
    }
}
