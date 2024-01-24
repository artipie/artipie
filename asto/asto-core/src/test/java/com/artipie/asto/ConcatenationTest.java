/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link Concatenation}.
 *
 * @since 0.17
 */
final class ConcatenationTest {

    @ParameterizedTest
    @MethodSource("flows")
    void shouldReadBytes(final Publisher<ByteBuffer> publisher, final byte[] bytes) {
        final Content content = new Content.OneTime(new Content.From(publisher));
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(content).single().blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(bytes)
        );
    }

    @ParameterizedTest
    @MethodSource("flows")
    void shouldReadBytesTwice(final Publisher<ByteBuffer> publisher, final byte[] bytes) {
        final Content content = new Content.From(publisher);
        final byte[] first = new Remaining(
            new Concatenation(content).single().blockingGet(),
            true
        ).bytes();
        final byte[] second = new Remaining(
            new Concatenation(content).single().blockingGet(),
            true
        ).bytes();
        MatcherAssert.assertThat(
            second,
            new IsEqual<>(first)
        );
    }

    @Test
    // @checkstyle MagicNumberCheck (25 lines)
    void shouldReadLargeContentCorrectly() {
        final int sizekb = 8;
        final int chunks = 128 * 1024 / sizekb + 1;
        final Content content = new Content.OneTime(
            new Content.From(
                subscriber -> {
                    IntStream.range(0, chunks).forEach(
                        unused -> subscriber.onNext(ByteBuffer.allocate(sizekb * 1024))
                    );
                    subscriber.onComplete();
                }
            )
        );
        final ByteBuffer result = new Concatenation(content).single().blockingGet();
        MatcherAssert.assertThat(
            result.limit(),
            new IsEqual<>(chunks * sizekb * 1024)
        );
        MatcherAssert.assertThat(
            result.capacity(),
            new IsEqual<>(2 * (chunks - 1) * sizekb * 1024)
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Object[]> flows() {
        final String data = "data";
        return Stream.of(
            new Object[] {Flowable.empty(), new byte[0]},
            new Object[] {Flowable.just(ByteBuffer.wrap(data.getBytes())), data.getBytes()},
            new Object[] {
                Flowable.just(
                    ByteBuffer.wrap("he".getBytes()),
                    ByteBuffer.wrap("ll".getBytes()),
                    ByteBuffer.wrap("o".getBytes())
                ),
                "hello".getBytes()
            }
        );
    }
}
