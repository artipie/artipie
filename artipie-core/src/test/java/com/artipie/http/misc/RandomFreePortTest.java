/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link RandomFreePort}.
 * @since 0.18
 */
final class RandomFreePortTest {
    @Test
    void returnsFreePort() {
        MatcherAssert.assertThat(
            new RandomFreePort().get(),
            new IsInstanceOf(Integer.class)
        );
    }
}
