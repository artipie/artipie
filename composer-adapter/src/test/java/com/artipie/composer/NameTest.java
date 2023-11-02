/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Name}.
 *
 * @since 0.1
 */
class NameTest {

    @Test
    void shouldGenerateKey() {
        MatcherAssert.assertThat(
            new Name("vendor/package").key().string(),
            Matchers.is("vendor/package.json")
        );
    }
}
