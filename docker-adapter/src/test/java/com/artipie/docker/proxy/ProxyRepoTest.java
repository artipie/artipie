/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.http.ResponseBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProxyRepo}.
 *
 * @since 0.3
 */
final class ProxyRepoTest {

    @Test
    void createsProxyLayers() {
        final ProxyRepo docker = new ProxyRepo(
            (line, headers, body) -> ResponseBuilder.ok().completedFuture(),
            "test"
        );
        MatcherAssert.assertThat(
            docker.layers(),
            new IsInstanceOf(ProxyLayers.class)
        );
    }

    @Test
    void createsProxyManifests() {
        final ProxyRepo docker = new ProxyRepo(
            (line, headers, body) -> ResponseBuilder.ok().completedFuture(),
            "my-repo"
        );
        MatcherAssert.assertThat(
            docker.manifests(),
            new IsInstanceOf(ProxyManifests.class)
        );
    }
}
