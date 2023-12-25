/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.reactivestreams.Publisher;

/**
 * Splits the original ByteBuffer to several ones
 * with size less or equals defined max size.
 *
 * @since 1.12.0
 */
public class Splitting {

    /**
     * Source byte buffer.
     */
    private final ByteBuffer source;

    /**
     * Max size of split byte buffer.
     */
    private final int size;

    /**
     * Ctor.
     *
     * @param source Source byte buffer.
     * @param size Max size of split byte buffer.
     */
    public Splitting(final ByteBuffer source, final int size) {
        this.source = source;
        this.size = size;
    }

    /**
     * Splits the original ByteBuffer to ones with size less
     * or equals defined max {@code size}.
     *
     * @return Publisher of ByteBuffers.
     */
    public Publisher<ByteBuffer> publisher() {
        final Publisher<ByteBuffer> res;
        int remaining = this.source.remaining();
        if (remaining > this.size) {
            final List<ByteBuffer> parts = new ArrayList<>(remaining / this.size + 1);
            while (remaining > 0) {
                final byte[] bytes;
                if (remaining > this.size) {
                    bytes = new byte[this.size];
                } else {
                    bytes = new byte[remaining];
                }
                this.source.get(bytes);
                parts.add(ByteBuffer.wrap(bytes));
                remaining = this.source.remaining();
            }
            res = Flowable.fromIterable(parts);
        } else {
            res = Flowable.just(this.source);
        }
        return res;
    }
}
