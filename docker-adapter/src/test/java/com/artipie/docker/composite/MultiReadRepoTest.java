/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * Tests for {@link MultiReadRepo}.
 *
 * @since 0.3
 */
final class MultiReadRepoTest {

    @Test
    void createsMultiReadLayers() {
        MatcherAssert.assertThat(
            new MultiReadRepo("one", new ArrayList<>()).layers(),
            new IsInstanceOf(MultiReadLayers.class)
        );
    }

    @Test
    void createsMultiReadManifests() {
        MatcherAssert.assertThat(
            new MultiReadRepo("two", new ArrayList<>()).manifests(),
            new IsInstanceOf(MultiReadManifests.class)
        );
    }
}
