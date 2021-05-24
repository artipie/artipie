/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.management.RepoPermissions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RepoPermissions.PathPattern}.
 *
 * @since 0.10
 */
class RepoPermissionsPathPatternTest {

    @ParameterizedTest
    @CsvSource({
        "**,true",
        "**\u002F*,true",
        "repo/**,true",
        "repo/**\u002F*,true",
        "*,false",
        "some/path/file.txt,false",
        "some/path/**\u002Ffile.txt,false"
    })
    void shouldValidate(final String expr, final boolean valid) {
        MatcherAssert.assertThat(
            new RepoPermissions.PathPattern(expr).valid("repo"),
            new IsEqual<>(valid)
        );
    }
}
