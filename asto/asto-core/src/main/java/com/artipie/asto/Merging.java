/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.nio.ByteBuffer;

/**
 * Merges ByteBuffer objects to bigger in the range of [minSize, maxSize].
 * Last block could be less than `minSize`.
 * Input ByteBuffer objects must be at most `maxSize` in `remaining` size.
 */
public class Merging {

    /**
     * Minimum output block size.
     */
    private final int minSize;

    /**
     * Count of bytes accumulated.
     */
    private int accumulated;

    /**
     * Data accumulator array.
     */
    private byte[] accumulator;

    /**
     * Ctor.
     *
     * @param minSize Minimal size of merged (accumulated) ByteBuffer.
     * @param maxSize Maximum size of merged (accumulated) ByteBuffer.
     */
    public Merging(final int minSize, final int maxSize) {
        this.minSize = minSize;
        this.accumulator = new byte[maxSize];
    }

    /**
     * Merge Flowable ByteBuffer objects and produce ByteBuffer objects with target size.
     * @param source Source of data blocks.
     * @return Flowable with merged blocks.
     */
    public Flowable<ByteBuffer> mergeFlow(final Flowable<ByteBuffer> source) {
        this.accumulated = 0;
        return source.concatMap(chunk -> {
            if (chunk.remaining() > this.accumulator.length) {
                throw new ArtipieIOException("Input chunk is bigger than maximum size");
            }
            final int diff = Math.min(chunk.remaining(), this.accumulator.length - accumulated);
            chunk.get(chunk.position(), accumulator, accumulated, diff);
            accumulated += diff;
            chunk.position(chunk.position() + diff);
            if (accumulated < this.minSize) {
                return Flowable.empty();
            }
            final ByteBuffer payload = ByteBuffer.wrap(accumulator, 0, accumulated);
            accumulated = 0;
            this.accumulator = new byte[this.accumulator.length];
            final int remaining = chunk.remaining();
            chunk.get(accumulator, accumulated, remaining);
            accumulated += remaining;
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
