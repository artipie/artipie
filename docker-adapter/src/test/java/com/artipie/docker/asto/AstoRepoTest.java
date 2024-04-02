/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepo}.
 */
final class AstoRepoTest {

    /**
     * Layers tested.
     */
    private Repo repo;

    @BeforeEach
    void setUp() {
        final RepoName name = new RepoName.Valid("test");
        this.repo = new AstoRepo(new InMemoryStorage(), name);
    }

    @Test
    void shouldCreateAstoLayers() {
        MatcherAssert.assertThat(
            this.repo.layers(),
            Matchers.instanceOf(AstoLayers.class)
        );
    }

    @Test
    void shouldCreateAstoManifests() {
        MatcherAssert.assertThat(
            this.repo.manifests(),
            Matchers.instanceOf(AstoManifests.class)
        );
    }
}
