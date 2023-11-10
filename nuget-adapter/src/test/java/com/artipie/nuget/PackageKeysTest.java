/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PackageKeys}.
 *
 * @since 0.1
 */
public class PackageKeysTest {

    @Test
    void shouldGenerateRootKey() {
        MatcherAssert.assertThat(
            new PackageKeys("Artipie.Module").rootKey().string(),
            new IsEqual<>("artipie.module")
        );
    }

    @Test
    void shouldGenerateVersionsKey() {
        MatcherAssert.assertThat(
            new PackageKeys("Newtonsoft.Json").versionsKey().string(),
            Matchers.is("newtonsoft.json/index.json")
        );
    }
}
