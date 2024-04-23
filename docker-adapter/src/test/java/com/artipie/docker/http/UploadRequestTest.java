/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.http.upload.UploadRequest;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UploadRequest}.
 */
class UploadRequestTest {

    @Test
    void shouldReadName() {
        MatcherAssert.assertThat(
            UploadRequest.from(new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/"))
                .name(),
            Matchers.is("my-repo")
        );
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            UploadRequest.from(new RequestLine(RqMethod.POST, String.format("/v2/%s/blobs/uploads/", name)))
                .name(),
            Matchers.is(name)
        );
    }

    @Test
    void shouldReadUuid() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.PATCH,
                "/v2/my-repo/blobs/uploads/a9e48d2a-c939-441d-bb53-b3ad9ab67709"
            )
        );
        MatcherAssert.assertThat(
            request.uuid(),
            Matchers.is("a9e48d2a-c939-441d-bb53-b3ad9ab67709")
        );
    }

    @Test
    void shouldReadEmptyUuid() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.PATCH, "/v2/my-repo/blobs/uploads//123")
        );
        MatcherAssert.assertThat(
            request.uuid(),
            new IsEqual<>("")
        );
    }

    @Test
    void shouldReadDigest() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.PUT,
                "/v2/my-repo/blobs/uploads/123-abc?digest=sha256:12345"
            )
        );
        MatcherAssert.assertThat(request.digest().string(), Matchers.is("sha256:12345"));
    }

    @Test
    void shouldThrowExceptionOnInvalidPath() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> UploadRequest.from(
                    new RequestLine(RqMethod.PUT, "/one/two")
                ).name()
            ).getMessage(),
            Matchers.containsString("Unexpected path")
        );
    }

    @Test
    void shouldThrowExceptionWhenDigestIsAbsent() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> UploadRequest.from(
                    new RequestLine(RqMethod.PUT,
                        "/v2/my-repo/blobs/uploads/123-abc?what=nothing"
                    )
                ).digest()
            ).getMessage(),
            Matchers.containsString("Request parameter `digest` is not exist")
        );
    }

    @Test
    void shouldReadMountWhenPresent() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.POST,
                "/v2/my-repo/blobs/uploads/?mount=sha256:12345&from=foo"
            )
        );
        MatcherAssert.assertThat(
            request.mount().orElseThrow().string(), Matchers.is("sha256:12345")
        );
    }

    @Test
    void shouldReadMountWhenAbsent() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/")
        );
        MatcherAssert.assertThat(
            request.mount().isEmpty(), Matchers.is(true)
        );
    }

    @Test
    void shouldReadFromWhenPresent() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.POST,
                "/v2/my-repo/blobs/uploads/?mount=sha256:12345&from=foo"
            )
        );
        MatcherAssert.assertThat(
            request.from().orElseThrow(), Matchers.is("foo")
        );
    }

    @Test
    void shouldReadFromWhenAbsent() {
        UploadRequest request = UploadRequest.from(
            new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/")
        );
        MatcherAssert.assertThat(
            request.from().isEmpty(), Matchers.is(true)
        );
    }
}
