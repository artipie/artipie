/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link UploadEntity.Request}.
 *
 * @since 0.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class UploadEntityRequestTest {

    @Test
    void shouldReadName() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/")
        );
        MatcherAssert.assertThat(request.name(), new IsEqual<>("my-repo"));
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            new UploadEntity.Request(
                new RequestLine(
                    RqMethod.POST, String.format("/v2/%s/blobs/uploads/", name)
                )
            ).name(),
            new IsEqual<>(name)
        );
    }

    @Test
    void shouldReadUuid() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(
                RqMethod.PATCH,
                "/v2/my-repo/blobs/uploads/a9e48d2a-c939-441d-bb53-b3ad9ab67709"
            )
        );
        MatcherAssert.assertThat(
            request.uuid(),
            new IsEqual<>("a9e48d2a-c939-441d-bb53-b3ad9ab67709")
        );
    }

    @Test
    void shouldReadEmptyUuid() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(RqMethod.PATCH, "/v2/my-repo/blobs/uploads//123")
        );
        MatcherAssert.assertThat(
            request.uuid(),
            new IsEqual<>("")
        );
    }

    @Test
    void shouldReadDigest() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(
                RqMethod.PUT,
                "/v2/my-repo/blobs/uploads/123-abc?digest=sha256:12345"
            )
        );
        MatcherAssert.assertThat(request.digest().string(), new IsEqual<>("sha256:12345"));
    }

    @Test
    void shouldThrowExceptionOnInvalidPath() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new UploadEntity.Request(
                    new RequestLine(RqMethod.PUT, "/one/two")
                ).name()
            ).getMessage(),
            new StringContains(false, "Unexpected path")
        );
    }

    @Test
    void shouldThrowExceptionWhenDigestIsAbsent() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> new UploadEntity.Request(
                    new RequestLine(
                        RqMethod.PUT,
                        "/v2/my-repo/blobs/uploads/123-abc?what=nothing"
                    )
                ).digest()
            ).getMessage(),
            new StringContains(false, "Unexpected query")
        );
    }

    @Test
    void shouldReadMountWhenPresent() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(
                RqMethod.POST,
                "/v2/my-repo/blobs/uploads/?mount=sha256:12345&from=foo"
            )
        );
        MatcherAssert.assertThat(
            request.mount().map(Digest::string),
            new IsEqual<>(Optional.of("sha256:12345"))
        );
    }

    @Test
    void shouldReadMountWhenAbsent() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/")
        );
        MatcherAssert.assertThat(
            request.mount().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldReadFromWhenPresent() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(
                RqMethod.POST,
                "/v2/my-repo/blobs/uploads/?mount=sha256:12345&from=foo"
            )
        );
        MatcherAssert.assertThat(
            request.from(),
            new IsEqual<>(Optional.of("foo"))
        );
    }

    @Test
    void shouldReadFromWhenAbsent() {
        final UploadEntity.Request request = new UploadEntity.Request(
            new RequestLine(RqMethod.POST, "/v2/my-repo/blobs/uploads/")
        );
        MatcherAssert.assertThat(
            request.from().isPresent(),
            new IsEqual<>(false)
        );
    }
}
