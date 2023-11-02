/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.misc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.core.IsEqual;

/**
 * Test for {@link SpaceInBeginning}.
 * @since 1.1.1
 */
final class SpaceInBeginningTest {
    @ParameterizedTest
    @CsvSource({
        "_entries:,0",
        "_  - maintainers,4",
        "_with_space_at_the_end   ,0",
        "_    four_space_both_sides    ,4"
    })
    void returnsPositionsOfSpaceAtBeginning(final String line, final int pos) {
        MatcherAssert.assertThat(
            new SpaceInBeginning(line.substring(1)).last(),
            new IsEqual<>(pos)
        );
    }
}
