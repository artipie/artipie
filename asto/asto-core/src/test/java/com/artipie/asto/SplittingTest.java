/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Splitting}.
 *
 * @since 1.12.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
public class SplittingTest {

    @Test
    void shouldReturnOneByteBufferWhenOriginalLessSize() {
        final byte[] data = new byte[12];
        new Random().nextBytes(data);
        final List<ByteBuffer> buffers = Flowable.fromPublisher(
            new Splitting(ByteBuffer.wrap(data), 24).publisher()
        ).toList().blockingGet();
        MatcherAssert.assertThat(buffers.size(), Matchers.equalTo(1));
        MatcherAssert.assertThat(
            new Remaining(buffers.get(0)).bytes(), Matchers.equalTo(data)
        );
    }

    @Test
    void shouldReturnOneByteBufferWhenOriginalEqualsSize() {
        final byte[] data = new byte[24];
        new Random().nextBytes(data);
        final List<ByteBuffer> buffers = Flowable.fromPublisher(
            new Splitting(ByteBuffer.wrap(data), 24).publisher()
        ).toList().blockingGet();
        MatcherAssert.assertThat(buffers.size(), Matchers.equalTo(1));
        MatcherAssert.assertThat(
            new Remaining(buffers.get(0)).bytes(), Matchers.equalTo(data)
        );
    }

    @Test
    void shouldReturnSeveralByteBuffersWhenOriginalMoreSize() {
        final byte[] data = new byte[2 * 24 + 8];
        new Random().nextBytes(data);
        final List<ByteBuffer> buffers = Flowable.fromPublisher(
            new Splitting(ByteBuffer.wrap(data), 24).publisher()
        ).toList().blockingGet();
        MatcherAssert.assertThat(buffers.size(), Matchers.equalTo(3));
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(
                    Flowable.fromIterable(buffers)
                ).single().blockingGet()
            ).bytes(), Matchers.equalTo(data)
        );
    }
}
