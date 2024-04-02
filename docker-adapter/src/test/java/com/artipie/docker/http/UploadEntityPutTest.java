/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.Upload;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link DockerSlice}.
 * Upload PUT endpoint.
 */
class UploadEntityPutTest {

    private Docker docker;

    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        final Storage storage = new InMemoryStorage();
        this.docker = new AstoDocker(storage);
        this.slice = new DockerSlice(this.docker);
    }

    @Test
    void shouldFinishUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads()
            .start()
            .toCompletableFuture().join();
        upload.append(new Content.From("data".getBytes()))
            .toCompletableFuture().join();
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "3a6eb0790f39ac87c94f3856b2dd2c5d110e6811602261a9a923d3bb23adc8b7"
        );
        final Response response = this.slice.response(
            UploadEntityPutTest.requestLine(name, upload.uuid(), digest),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            "Returns 201 status and corresponding headers",
            response,
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", String.format("/v2/%s/blobs/%s", name, digest)),
                new Header("Content-Length", "0"),
                new Header("Docker-Content-Digest", digest)
            )
        );
        MatcherAssert.assertThat(
            "Puts blob into storage",
            this.docker.repo(new RepoName.Simple(name)).layers().get(new Digest.FromString(digest))
                .thenApply(Optional::isPresent)
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsBadRequestWhenDigestsDoNotMatch() {
        final String name = "repo";
        final byte[] content = "something".getBytes();
        final Upload upload = this.docker.repo(new RepoName.Valid(name)).uploads().start()
            .toCompletableFuture().join();
        upload.append(new Content.From(content)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Returns 400 status",
            this.slice,
            new SliceHasResponse(
                new IsErrorsResponse(RsStatus.BAD_REQUEST, "DIGEST_INVALID"),
                UploadEntityPutTest.requestLine(name, upload.uuid(), "sha256:0000")
            )
        );
        MatcherAssert.assertThat(
            "Does not put blob into storage",
            this.docker.repo(new RepoName.Simple(name)).layers().get(new Digest.Sha256(content))
                .thenApply(Optional::isPresent)
                .toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldReturnNotFoundWhenUploadNotExists() {
        final Response response = this.slice.response(
            new RequestLine(RqMethod.PUT, "/v2/test/blobs/uploads/12345"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        MatcherAssert.assertThat(
            response,
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UPLOAD_UNKNOWN")
        );
    }

    /**
     * Returns request line.
     * @param name Repo name
     * @param uuid Upload uuid
     * @param digest Digest
     * @return RequestLine instance
     */
    private static RequestLine requestLine(
        final String name,
        final String uuid,
        final String digest
    ) {
        return new RequestLine(
            RqMethod.PUT,
            String.format("/v2/%s/blobs/uploads/%s?digest=%s", name, uuid, digest)
        );
    }

}
