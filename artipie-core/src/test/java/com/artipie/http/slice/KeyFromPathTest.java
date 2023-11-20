/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyFromPath}.
 *
 * @since 0.6
 */
final class KeyFromPathTest {

    @Test
    void removesLeadingSlashes() {
        MatcherAssert.assertThat(
            new KeyFromPath("/foo/bar").string(),
            new IsEqual<>("foo/bar")
        );
    }

    @Test
    void usesRelativePathsSlashes() {
        final String rel = "one/two";
        MatcherAssert.assertThat(
            new KeyFromPath(rel).string(),
            new IsEqual<>(rel)
        );
    }
}
