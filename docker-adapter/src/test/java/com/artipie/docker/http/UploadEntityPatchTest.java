/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
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
 * Upload PATCH endpoint.
 */
class UploadEntityPatchTest {

    private Docker docker;

    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice(this.docker);
    }

    @Test
    void shouldReturnUpdatedUploadStatus() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads()
            .start()
            .toCompletableFuture().join();
        final String uuid = upload.uuid();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, uuid);
        final byte[] data = "data".getBytes();
        final Response response = this.slice.response(
            new RequestLine(RqMethod.PATCH, String.format("%s", path)),
            Headers.EMPTY,
            new Content.From(data)
        );
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(
                RsStatus.ACCEPTED,
                new Header("Location", path),
                new Header("Range", String.format("0-%d", data.length - 1)),
                new Header("Content-Length", "0"),
                new Header("Docker-Upload-UUID", uuid)
            )
        );
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine(RqMethod.PATCH, "/v2/test/blobs/uploads/12345"),
            Headers.EMPTY,
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UPLOAD_UNKNOWN")
        );
    }
}
