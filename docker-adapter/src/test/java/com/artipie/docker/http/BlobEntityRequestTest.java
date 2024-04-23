/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.http.blobs.BlobsRequest;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BlobsRequest}.
 */
class BlobEntityRequestTest {

    @Test
    void shouldReadName() {
        final String name = "my-repo";
        MatcherAssert.assertThat(
            BlobsRequest.from(
                new RequestLine(
                    RqMethod.HEAD, String.format("/v2/%s/blobs/sha256:098", name)
                )
            ).name(),
            Matchers.is(name)
        );
    }

    @Test
    void shouldReadDigest() {
        final String digest = "sha256:abc123";
        MatcherAssert.assertThat(
            BlobsRequest.from(
                new RequestLine(
                    RqMethod.GET, String.format("/v2/some-repo/blobs/%s", digest)
                )
            ).digest().string(),
            Matchers.is(digest)
        );
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            BlobsRequest.from(
                new RequestLine(
                    RqMethod.HEAD, String.format("/v2/%s/blobs/sha256:234434df", name)
                )
            ).name(),
            Matchers.is(name)
        );
    }

}
