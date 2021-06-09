/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Key;
import java.util.Optional;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.llorllale.cactoos.matchers.IsTrue;

/**
 * Tests for {@link Layout}.
 *
 * @since 0.16
 */
final class LayoutTest {

    @Test
    void alwaysAllowsDashboard() {
        MatcherAssert.assertThat(
            new Layout.Org().hasDashboard(),
            new IsTrue()
        );
    }

    @Test
    void containsCorrectPattern() {
        MatcherAssert.assertThat(
            new Layout.Org().pattern().pattern(),
            Matchers.is(
                Pattern.compile("/(?:[^/.]+)/(?:[^/.]+)(/.*)?").pattern()
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/foo",
        ""
    })
    void emptyForPathOfLessThanTwoParts(final String path) {
        MatcherAssert.assertThat(
            new Layout.Org().keyFromPath(path),
            new IsEqual<>(Optional.empty())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "foo/bar/baz/favicon.ico",
        "foo/bar/robots.txt",
        "foo/bar"
    })
    void extractsKey(final String path) {
        MatcherAssert.assertThat(
            new Layout.Org().keyFromPath(path),
            new IsEqual<>(
                Optional.of(new Key.From("foo/bar"))
            )
        );
    }
}
