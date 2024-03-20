/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Blob Head endpoint.
 */
class BlobEntityHeadTest {

    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice(new AstoDocker(new ExampleStorage()));
    }

    @Test
    void shouldFindLayer() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "aad63a9339440e7c3e1fff2b988991b9bfb81280042fa7f39a5e327023056819"
        );
        final Response response = this.slice.response(
            new RequestLine(
                RqMethod.HEAD,
                String.format("/v2/test/blobs/%s", digest)
            ),
            Headers.EMPTY,
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(
                RsStatus.OK,
                new Header("Content-Length", "2803255"),
                new Header("Docker-Content-Digest", digest),
                new Header("Content-Type", "application/octet-stream")
            )
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownDigest() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    RqMethod.HEAD,
                    String.format(
                        "/v2/test/blobs/%s",
                        "sha256:0123456789012345678901234567890123456789012345678901234567890123"
                    )
                ),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UNKNOWN")
        );
    }
}
