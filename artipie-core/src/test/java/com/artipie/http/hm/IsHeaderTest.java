/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IsHeader}.
 *
 * @since 0.8
 */
class IsHeaderTest {

    @Test
    void shouldMatchEqual() {
        final String name = "Content-Length";
        final String value = "100";
        final IsHeader matcher = new IsHeader(name, value);
        MatcherAssert.assertThat(
            matcher.matches(new MapEntry<>(name, value)),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldMatchUsingValueMatcher() {
        final IsHeader matcher = new IsHeader(
            "content-type", new StringStartsWith(false, "text/plain")
        );
        MatcherAssert.assertThat(
            matcher.matches(
                new MapEntry<>("Content-Type", "text/plain; charset=us-ascii")
            ),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldNotMatchNotEqual() {
        MatcherAssert.assertThat(
            new IsHeader("name", "value").matches(new MapEntry<>("n", "v")),
            new IsEqual<>(false)
        );
    }
}
