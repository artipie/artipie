/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Docker;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.Upload;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Upload GET endpoint.
 */
public final class UploadEntityGetTest {

    private Docker docker;

    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
        this.slice = new DockerSlice(this.docker);
    }

    @Test
    void shouldReturnZeroOffsetAfterUploadStarted() {
        final String name = "test";
        final Upload upload = this.docker.repo(name)
            .uploads()
            .start()
            .toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, path),
            Headers.EMPTY, Content.EMPTY
        ).join();
        ResponseAssert.check(
            response,
            RsStatus.NO_CONTENT,
            new Header("Range", "0-0"),
            new Header("Content-Length", "0"),
            new Header("Docker-Upload-UUID", upload.uuid())
        );
    }

    @Test
    void shouldReturnZeroOffsetAfterOneByteUploaded() {
        final String name = "test";
        final Upload upload = this.docker.repo(name)
            .uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(new Content.From(new byte[1])).toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, path), Headers.EMPTY, Content.EMPTY
        ).join();
        ResponseAssert.check(
            response,
            RsStatus.NO_CONTENT,
            new Header("Range", "0-0"),
            new Header("Content-Length", "0"),
            new Header("Docker-Upload-UUID", upload.uuid())
        );
    }

    @Test
    void shouldReturnOffsetDuringUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(name)
            .uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(new Content.From(new byte[128])).toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response get = this.slice.response(
            new RequestLine(RqMethod.GET, path), Headers.EMPTY, Content.EMPTY
        ).join();
        ResponseAssert.check(
            get,
            RsStatus.NO_CONTENT,
            new Header("Range", "0-127"),
            new Header("Content-Length", "0"),
            new Header("Docker-Upload-UUID", upload.uuid())
        );
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine(RqMethod.GET, "/v2/test/blobs/uploads/12345"),
            Headers.EMPTY, Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            response,
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UPLOAD_UNKNOWN")
        );
    }
}
