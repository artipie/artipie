/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.artipie.docker.ref.ManifestRef;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ManifestPath}.
 *
 * @since 0.3
 */
class ManifestPathTest {

    @Test
    void shouldBuildPathString() {
        final ManifestPath path = new ManifestPath(
            new RepoName.Valid("some/image"),
            new ManifestRef.FromString("my-ref")
        );
        MatcherAssert.assertThat(
            path.string(),
            new IsEqual<>("/v2/some/image/manifests/my-ref")
        );
    }
}
