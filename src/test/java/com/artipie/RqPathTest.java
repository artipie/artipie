/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RqPath}.
 * @since 0.23
 */
class RqPathTest {

    @ParameterizedTest
    @CsvSource({
        "/t/ol-4ee312d8-9fe2-44d2-bea9-053325e1ffd5/my-conda/noarch/repodata.json,true",
        // @checkstyle LineLengthCheck (1 line)
        "/t/ol-4ee312d8-9fe2-44d2-bea9-053325e1ffd5/username/my-conda/linux-64/current_repodata.json,true",
        "/t/any/my-repo/repodata.json,false",
        "/t/a/v/any,false"
    })
    void testsPath(final String path, final boolean res) {
        MatcherAssert.assertThat(
            RqPath.CONDA.test(path),
            new IsEqual<>(res)
        );
    }

}
