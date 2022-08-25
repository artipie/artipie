/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.asto.Key;
import com.artipie.settings.repo.PathPattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link Layout.Flat}.
 *
 * @since 0.15
 */
final class LayoutFlatTest {

    @Test
    void usesFlatPattern() {
        MatcherAssert.assertThat(
            new Layout.Flat().pattern(),
            new IsEqual<>(new PathPattern("flat").pattern())
        );
    }

    @ParameterizedTest
    @CsvSource({"/one,one", "/some/path,some"})
    void getKeyFromPath(final String path, final String key) {
        MatcherAssert.assertThat(
            new Layout.Flat().keyFromPath(path).get(),
            new IsEqual<>(new Key.From(key))
        );
    }

    @Test
    void getEmptyKeyFromEmptyPath() {
        MatcherAssert.assertThat(
            new Layout.Flat().keyFromPath("").isEmpty(),
            new IsEqual<>(true)
        );
    }
}
