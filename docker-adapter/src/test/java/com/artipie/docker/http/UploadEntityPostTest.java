/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.google.common.base.Strings;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Upload PUT endpoint.
 */
class UploadEntityPostTest {

    /**
     * Docker instance used in tests.
     */
    private Docker docker;

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice(this.docker);
    }

    @Test
    void shouldStartUpload() {
        uploadStartedAssert(
            this.slice.response(
                new RequestLine(RqMethod.POST, "/v2/test/blobs/uploads/"),
                Headers.EMPTY,
                Content.EMPTY
            ).join()
        );
    }

    @Test
    void shouldStartUploadIfMountNotExists() {
        uploadStartedAssert(
            new DockerSlice(this.docker).response(
                new RequestLine(
                    RqMethod.POST,
                    "/v2/test/blobs/uploads/?mount=sha256:123&from=test"
                ), Headers.EMPTY, Content.EMPTY
            ).join()
        );
    }

    @Test
    void shouldMountBlob() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"
        );
        final String from = "my-alpine";
        this.docker.repo(new RepoName.Simple(from)).layers().put(
            new TrustedBlobSource("data".getBytes())
        ).toCompletableFuture().join();
        final String name = "test";
        MatcherAssert.assertThat(
            this.slice,
            new SliceHasResponse(
                new ResponseMatcher(
                    RsStatus.CREATED,
                    new Header("Location", String.format("/v2/%s/blobs/%s", name, digest)),
                    new Header("Content-Length", "0"),
                    new Header("Docker-Content-Digest", digest)
                ),
                new RequestLine(
                    RqMethod.POST,
                    String.format("/v2/%s/blobs/uploads/?mount=%s&from=%s", name, digest, from)
                )
            )
        );
    }

    private static void uploadStartedAssert(ResponseImpl actual) {
        Assertions.assertEquals("0-0", actual.headers().single("Range").getValue());
        Assertions.assertEquals("0", actual.headers().single("Content-Length").getValue());
        Assertions.assertTrue(
            actual.headers().single("Location").getValue()
                .startsWith("/v2/test/blobs/uploads/")
        );
        Assertions.assertFalse(
            Strings.isNullOrEmpty(actual.headers().single("Docker-Upload-UUID").getValue())
        );
    }
}
