/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BlobEntity.Request}.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class BlobEntityRequestTest {

    @Test
    void shouldReadName() {
        final String name = "my-repo";
        MatcherAssert.assertThat(
            new BlobEntity.Request(
                new RequestLine(
                    RqMethod.HEAD, String.format("/v2/%s/blobs/sha256:098", name)
                )
            ).name(),
            new IsEqual<>(name)
        );
    }

    @Test
    void shouldReadDigest() {
        final String digest = "sha256:abc123";
        MatcherAssert.assertThat(
            new BlobEntity.Request(
                new RequestLine(
                    RqMethod.GET, String.format("/v2/some-repo/blobs/%s", digest)
                )
            ).digest().string(),
            new IsEqual<>(digest)
        );
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            new BlobEntity.Request(
                new RequestLine(
                    RqMethod.HEAD, String.format("/v2/%s/blobs/sha256:234434df", name)
                )
            ).name(),
            new IsEqual<>(name)
        );
    }

}
