/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;

/**
 * Concatenation of {@link ByteBuffer} instances.
 *
 * @since 0.17
 */
public class Concatenation {

    /**
     * Source of byte buffers.
     */
    private final Publisher<ByteBuffer> source;

    /**
     * Ctor.
     *
     * @param source Source of byte buffers.
     */
    public Concatenation(final Publisher<ByteBuffer> source) {
        this.source = source;
    }

    /**
     * Concatenates all buffers into single one.
     *
     * @return Single buffer.
     */
    public Single<ByteBuffer> single() {
        return Flowable.fromPublisher(this.source).reduce(
            ByteBuffer.allocate(0),
            (left, right) -> {
                right.mark();
                final ByteBuffer result;
                if (left.capacity() - left.limit() >= right.limit()) {
                    left.position(left.limit());
                    left.limit(left.limit() + right.limit());
                    result = left.put(right);
                } else {
                    result = ByteBuffer.allocate(
                        2 * Math.max(left.capacity(), right.capacity())
                    ).put(left).put(right);
                }
                right.reset();
                result.flip();
                return result;
            }
        );
    }
}
