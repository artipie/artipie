/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Digest;
import com.artipie.docker.error.BlobUnknownError;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ErrorsResponse}.
 *
 * @since 0.5
 */
public final class ErrorsResponseTest {

    @Test
    void shouldHaveExpectedStatus() {
        final RsStatus status = RsStatus.NOT_FOUND;
        MatcherAssert.assertThat(
            new ErrorsResponse(status, Collections.emptyList()),
            new RsHasStatus(status)
        );
    }

    @Test
    void shouldHaveExpectedBody() {
        MatcherAssert.assertThat(
            new ErrorsResponse(
                RsStatus.NOT_FOUND,
                Collections.singleton(new BlobUnknownError(new Digest.Sha256("123")))
            ),
            new RsHasBody(
                "{\"errors\":[{\"code\":\"BLOB_UNKNOWN\",\"message\":\"blob unknown to registry\",\"detail\":\"sha256:123\"}]}".getBytes()
            )
        );
    }
}
