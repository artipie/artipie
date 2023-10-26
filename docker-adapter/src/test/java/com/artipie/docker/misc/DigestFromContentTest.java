/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DigestFromContent}.
 * @since 0.2
 */
class DigestFromContentTest {

    @Test
    void calculatesHexCorrectly() {
        final byte[] data = "abc123".getBytes();
        MatcherAssert.assertThat(
            new DigestFromContent(new Content.From(data))
                .digest().toCompletableFuture().join().hex(),
            new IsEqual<>(DigestUtils.sha256Hex(data))
        );
    }

}
