/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.RepoName;
import java.util.ArrayList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MultiReadRepo}.
 *
 * @since 0.3
 */
final class MultiReadRepoTest {

    @Test
    void createsMultiReadLayers() {
        MatcherAssert.assertThat(
            new MultiReadRepo(new RepoName.Simple("one"), new ArrayList<>(0)).layers(),
            new IsInstanceOf(MultiReadLayers.class)
        );
    }

    @Test
    void createsMultiReadManifests() {
        MatcherAssert.assertThat(
            new MultiReadRepo(new RepoName.Simple("two"), new ArrayList<>(0)).manifests(),
            new IsInstanceOf(MultiReadManifests.class)
        );
    }
}
