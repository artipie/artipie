/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.docker.error.BlobUnknownError;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.hm.RsHasBody;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@code com.artipie.docker.error.DockerError.json()}.
 */
public final class ErrorsResponseTest {
    @Test
    void shouldHaveExpectedBody() {
        MatcherAssert.assertThat(
            ResponseBuilder.notFound()
                .jsonBody(new BlobUnknownError(new Digest.Sha256("123")).json())
                .build(),
            new RsHasBody(
                "{\"errors\":[{\"code\":\"BLOB_UNKNOWN\",\"message\":\"blob unknown to registry\",\"detail\":\"sha256:123\"}]}".getBytes()
            )
        );
    }
}
