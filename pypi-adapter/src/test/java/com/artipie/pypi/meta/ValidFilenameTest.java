/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.pypi.meta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link ValidFilename}.
 * @since 0.6
 * @checkstyle ParameterNumberCheck (500 lines)
 */
class ValidFilenameTest {

    @ParameterizedTest
    @CsvSource({
        "my-project,0.3,my-project-0.3.tar.gz,true",
        "my-project,0.3,my-project-0.4.tar,false",
        "Another_project,123.93,Another_project-123.93.zip,true",
        "very-difficult-project,1.0a2,very-difficult-project-1.0a2-py3-any-none.whl,true",
        "one,0.0.1,two-0.2.tar.gz,false"
    })
    void checks(final String name, final String version, final String filename, final boolean res) {
        MatcherAssert.assertThat(
            new ValidFilename(
                new PackageInfo.FromMetadata(this.metadata(name, version)), filename
            ).valid(),
            new IsEqual<>(res)
        );
    }

    private String metadata(final String name, final String version) {
        return String.join(
            "\n",
            String.format("Name: %s", name),
            String.format("Version: %s", version)
        );
    }

}
