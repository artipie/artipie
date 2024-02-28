/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.nio.ByteBuffer;

/**
 * Merges ByteBuffer objects to bigger in the range of [size, size * 2).
 * Last block could be less than `size`.
 * Input buffers must be at most `size` in `remaining` size.
 */
public class Merging {

    /**
     * Data accumulator array.
     */
    private final byte[] accumulator;

    /**
     * Minimal block size.
     */
    private final int size;

    /**
     * Count of bytes accumulated.
     */
    private int accumulated;

    /**
     * Ctor.
     *
     * @param size Minimal size of merged (accumulated) ByteBuffer.
     */
    public Merging(final int size) {
        this.size = size;
        this.accumulator = new byte[this.size * 2];
    }

    /**
     * Merge Flowable ByteBuffer objects and produce ByteBuffer objects with target size.
     * @param source Source of data blocks.
     * @return Flowable with merged blocks.
     */
    public Flowable<ByteBuffer> mergeFlow(final Flowable<ByteBuffer> source) {
        this.accumulated = 0;
        return source.concatMap(chunk -> {
            final int remaining = chunk.remaining();
            if (remaining > this.size) {
                throw new ArtipieIOException("Input chunk is bigger than specified size");
            }
            chunk.get(accumulator, accumulated, remaining);
            accumulated += remaining;
            if (accumulated < this.size) {
                return Flowable.empty();
            }
            final ByteBuffer payload = ByteBuffer.wrap(accumulator, 0, accumulated);
            accumulated = 0;
            return Flowable.just(payload);
        }).concatWith(Maybe.defer(() -> {
            if (accumulated == 0) {
                return Maybe.empty();
            }
            final ByteBuffer payload = ByteBuffer.wrap(accumulator, 0, accumulated);
            accumulated = 0;
            return Maybe.just(payload);
        }));
    }
}
