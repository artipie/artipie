/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BlobPath}.
 *
 * @since 0.3
 */
class BlobPathTest {

    @Test
    void shouldBuildPathString() {
        final BlobPath path = new BlobPath(
            new RepoName.Valid("my/thing"),
            new Digest.FromString("sha256:12345")
        );
        MatcherAssert.assertThat(
            path.string(),
            new IsEqual<>("/v2/my/thing/blobs/sha256:12345")
        );
    }
}
