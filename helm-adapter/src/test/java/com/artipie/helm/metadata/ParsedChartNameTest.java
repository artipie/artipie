/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.metadata;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link ParsedChartName}.
 * @since 0.3
 */
final class ParsedChartNameTest {
    @ParameterizedTest
    @ValueSource(strings = {"name:", " name_with_space_before:", " space_both: "})
    void returnsValidForCorrectName(final String name) {
        MatcherAssert.assertThat(
            new ParsedChartName(name).valid(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"without_colon", " - starts_with_dash:", "entries:"})
    void returnsNotValidForMalformedName(final String name) {
        MatcherAssert.assertThat(
            new ParsedChartName(name).valid(),
            new IsEqual<>(false)
        );
    }
}
