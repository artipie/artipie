/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.proxy.ProxyRepo;
import com.artipie.http.ResponseBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link CacheRepo}.
 *
 * @since 0.3
 */
final class CacheRepoTest {

    /**
     * Tested {@link CacheRepo}.
     */
    private CacheRepo repo;

    @BeforeEach
    void setUp() {
        this.repo = new CacheRepo(
            "test",
            new ProxyRepo(
                (line, headers, body) -> ResponseBuilder.ok().completedFuture(),
                "test-origin"
            ),
            new AstoDocker(new InMemoryStorage())
                .repo("test-cache"), Optional.empty(), "*"
        );
    }

    @Test
    void createsCacheLayers() {
        MatcherAssert.assertThat(
            this.repo.layers(),
            new IsInstanceOf(CacheLayers.class)
        );
    }

    @Test
    void createsCacheManifests() {
        MatcherAssert.assertThat(
            this.repo.manifests(),
            new IsInstanceOf(CacheManifests.class)
        );
    }
}
