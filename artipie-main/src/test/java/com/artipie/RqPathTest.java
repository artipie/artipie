/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
        "/t/ol-4ee312d8-9fe2-44d2-bea9-053325e1ffd5/username/my-conda/linux-64/current_repodata.json,true",
        "/t/any/my-repo/repodata.json,false",
        "/t/a/v/any,false",
        "/t/ol-4ee312d8-9fe2-44d2-bea9-053325e1ffd5/my-conda/win64/some-package-0.1-0.conda,true",
        "/t/user-token/my-conda/noarch/myTest-0.2-0.tar.bz2,true",
        "/usernane/my-repo/win54/package-0.0.3-0.tar.bz2,false"
    })
    void testsPath(final String path, final boolean res) {
        MatcherAssert.assertThat(
            RqPath.CONDA.test(path),
            new IsEqual<>(res)
        );
    }

}
