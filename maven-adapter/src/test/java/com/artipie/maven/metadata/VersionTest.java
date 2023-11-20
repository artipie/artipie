/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.metadata;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Version}.
 * @since 0.5
 */
class VersionTest {

    @CsvSource({
        "1,1,0",
        "1,2,-1",
        "2,1,1",
        "0.2,0.20.1,-1",
        "1.0,1.1-SNAPSHOT,-1",
        "2.0-SNAPSHOT,1.1,1",
        "0.1-SNAPSHOT,0.3-SNAPSHOT,-1",
        "1.0.1,0.1,1",
        "1.1-alpha-2,1.1,-1",
        "1.1-alpha-2,1.1-alpha-3,-1"
    })
    @ParameterizedTest
    void comparesSimpleVersions(final String first, final String second, final int res) {
        MatcherAssert.assertThat(
            new Version(first).compareTo(new Version(second)),
            new IsEqual<>(res)
        );
    }

}
