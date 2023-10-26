/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.docker.Digest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link BlobKey}.
 *
 * @since 0.2
 */
public final class BlobKeyTest {

    @Test
    public void buildsValidPathFromDigest() {
        final String hex = "00801519ca78ec3ac54f0aea959bce240ab3b42fae7727d2359b1f9ebcabe23d";
        MatcherAssert.assertThat(
            new BlobKey(new Digest.Sha256(hex)).string(),
            Matchers.equalTo(
                String.join(
                    "/",
                    "blobs", "sha256", "00", hex, "data"
                )
            )
        );
    }
}
