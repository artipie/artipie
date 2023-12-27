/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Content;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ContentDigest}.
 *
 * @since 0.22
 */
final class ContentDigestTest {

    @Test
    void calculatesHex() throws Exception {
        MatcherAssert.assertThat(
            new ContentDigest(
                new Content.OneTime(
                    new Content.From(
                        // @checkstyle MagicNumberCheck (1 line)
                        new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe}
                    )
                ),
                Digests.SHA256
            ).hex().toCompletableFuture().get(),
            new IsEqual<>("65ab12a8ff3263fbc257e5ddf0aa563c64573d0bab1f1115b9b107834cfa6971")
        );
    }
}
