/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

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
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UploadEntity.Delete}.
 * Upload DElETE endpoint.
 *
 * @since 0.16
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UploadEntityDeleteTest {
    /**
     * Docker registry used in tests.
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
    void shouldCancelUpload() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .uploads()
            .start()
            .toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response get = this.slice.response(
            new RequestLine(RqMethod.DELETE, String.format("%s", path)).toString(),
            Headers.EMPTY,
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            get,
            new ResponseMatcher(
                RsStatus.OK,
                new Header("Docker-Upload-UUID", upload.uuid())
            )
        );
    }

    @Test
    void shouldNotCancelUploadTwice() {
        final String name = "test";
        final Upload upload = this.docker.repo(new RepoName.Valid(name))
            .uploads()
            .start()
            .toCompletableFuture().join();
        upload.cancel().toCompletableFuture().join();
        final String path = String.format("/v2/%s/blobs/uploads/%s", name, upload.uuid());
        final Response get = this.slice.response(
            new RequestLine(RqMethod.DELETE, String.format("%s", path)).toString(),
            Headers.EMPTY,
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            get,
            new IsErrorsResponse(RsStatus.NOT_FOUND, "BLOB_UPLOAD_UNKNOWN")
        );
    }

}
