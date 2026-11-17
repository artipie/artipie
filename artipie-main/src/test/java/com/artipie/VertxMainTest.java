/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link VertxMain}.
 * @since 1.0
 */
final class VertxMainTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 8086, 65_535})
    void enablesRestApiForPositivePort(final int port) {
        MatcherAssert.assertThat(VertxMain.restApiEnabled(port), new IsEqual<>(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void disablesRestApiForZeroOrNegativePort(final int port) {
        MatcherAssert.assertThat(VertxMain.restApiEnabled(port), new IsEqual<>(false));
    }
}
