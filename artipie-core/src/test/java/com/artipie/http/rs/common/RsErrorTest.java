/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs.common;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RsError}.
 * @since 1.0
 */
final class RsErrorTest {

    @Test
    void rsErrorHasStatus() {
        MatcherAssert.assertThat(
            new RsError(new ArtipieHttpException(RsStatus.FORBIDDEN)),
            new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }

    @Test
    void rsWithInternalError() {
        MatcherAssert.assertThat(
            new RsError(new IOException()),
            new RsHasStatus(RsStatus.INTERNAL_ERROR)
        );
    }

    @Test
    void rsErrorMessageIsBody() {
        final String msg = "something goes wrong";
        MatcherAssert.assertThat(
            new RsError(new ArtipieHttpException(RsStatus.INTERNAL_ERROR, msg)),
            new RsHasBody(String.format("%s\n", msg), StandardCharsets.UTF_8)
        );
    }

    @Test
    void rsErrorSuppressedAddToBody() {
        final Exception cause = new ArtipieException("main cause");
        cause.addSuppressed(new ArtipieIOException("suppressed cause"));
        MatcherAssert.assertThat(
            new RsError(new ArtipieHttpException(RsStatus.INTERNAL_ERROR, cause)),
            new RsHasBody(
                "Internal Server Error\nmain cause\njava.io.IOException: suppressed cause\n",
                StandardCharsets.UTF_8
            )
        );
    }
}
