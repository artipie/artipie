/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link BufAccumulator}.
 * @since 1.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class BufAccumulatorTest {

    @Test
    void concatChunks() {
        try (BufAccumulator acc = new BufAccumulator(1)) {
            acc.write(ByteBuffer.wrap("Hello ".getBytes()));
            acc.write(ByteBuffer.wrap("chunk".getBytes()));
            acc.write(ByteBuffer.wrap("!".getBytes()));
            MatcherAssert.assertThat(new String(acc.array()), new IsEqual<>("Hello chunk!"));
        }
    }

    @Test
    void dropBytes() {
        try (BufAccumulator acc = new BufAccumulator(1)) {
            acc.write(ByteBuffer.wrap(new byte[]{0, 1, 2, 3}));
            acc.drop(2);
            MatcherAssert.assertThat(acc.array(), new IsEqual<>(new byte[]{2, 3}));
        }
    }

    @Test
    void readBuffers() {
        try (BufAccumulator acc = new BufAccumulator(1)) {
            acc.write(ByteBuffer.wrap(new byte[]{0x0a, 0x0b, 0x0c}));
            final ByteBuffer first = ByteBuffer.allocate(2);
            final int one = acc.read(first);
            first.flip();
            MatcherAssert.assertThat(
                "first read has two first bytes",
                bufArray(first), Matchers.equalTo(new byte[]{0x0a, 0x0b})
            );
            MatcherAssert.assertThat(
                "first read amount is 2",
                one, Matchers.is(2)
            );
            acc.write(ByteBuffer.wrap(new byte[]{0x0d, 0x0e, 0x0f}));
            final ByteBuffer second = ByteBuffer.allocate(3);
            final int two = acc.read(second);
            second.flip();
            MatcherAssert.assertThat(
                "second read has next three bytes",
                bufArray(second), Matchers.equalTo(new byte[]{0x0c, 0x0d, 0x0e})
            );
            MatcherAssert.assertThat(
                "second read amount is 3",
                two, Matchers.is(3)
            );
            final ByteBuffer third = ByteBuffer.allocate(4);
            final int three = acc.read(third);
            third.flip();
            MatcherAssert.assertThat(
                "third read has last one byte",
                bufArray(third), Matchers.equalTo(new byte[]{0x0f})
            );
            MatcherAssert.assertThat(
                "second read amount is 1",
                three, Matchers.is(1)
            );
        }
    }

    /**
     * Array from buffer.
     * @param buf Buffer
     * @return Array
     */
    private static byte[] bufArray(final ByteBuffer buf) {
        final byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        return arr;
    }
}
