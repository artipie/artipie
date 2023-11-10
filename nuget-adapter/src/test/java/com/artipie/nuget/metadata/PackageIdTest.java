/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PackageId}.
 * @since 0.6
 */
class PackageIdTest {

    @Test
    void shouldPreserveOriginal() {
        final String id = "Microsoft.Extensions.Logging";
        MatcherAssert.assertThat(
            new PackageId(id).raw(),
            Matchers.is(id)
        );
    }

    @Test
    void shouldGenerateLower() {
        MatcherAssert.assertThat(
            new PackageId("My.Lib").normalized(),
            Matchers.is("my.lib")
        );
    }

}
