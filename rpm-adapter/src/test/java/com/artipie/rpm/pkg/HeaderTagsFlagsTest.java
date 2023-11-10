/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test for {@link HeaderTags.Flags}.
 * @since 1.10
 */
class HeaderTagsFlagsTest {

    @ParameterizedTest
    @EnumSource(HeaderTags.Flags.class)
    void findsFlagsByCode(final HeaderTags.Flags flag) {
        MatcherAssert.assertThat(
            HeaderTags.Flags.find(flag.code()).get(),
            new IsEqual<>(flag.notation())
        );
    }

    @Test
    void returnsEmptyWhenNotFound() {
        MatcherAssert.assertThat(
            HeaderTags.Flags.find(0).isPresent(),
            new IsEqual<>(false)
        );
    }

}
