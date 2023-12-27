/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Content;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link PublisherAs}.
 * @since 0.24
 */
class PublisherAsTest {

    @Test
    void readsBytes() {
        final byte[] buf = "abc".getBytes();
        MatcherAssert.assertThat(
            new PublisherAs(new Content.From(buf)).bytes().toCompletableFuture().join(),
            new IsEqual<>(buf)
        );
    }

    @Test
    void readsAsciiString() {
        final byte[] buf = "абв".getBytes(StandardCharsets.US_ASCII);
        MatcherAssert.assertThat(
            new PublisherAs(new Content.From(buf)).asciiString().toCompletableFuture().join(),
            new IsEqual<>(new String(buf, StandardCharsets.US_ASCII))
        );
    }

    @Test
    void readsString() {
        final byte[] buf = "фыв".getBytes(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            new PublisherAs(new Content.From(buf)).string(StandardCharsets.UTF_8)
                .toCompletableFuture().join(),
            new IsEqual<>(new String(buf, StandardCharsets.UTF_8))
        );
    }

}
