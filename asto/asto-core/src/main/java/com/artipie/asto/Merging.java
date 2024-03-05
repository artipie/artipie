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
     * Maximum output block size.
     */
    private final int maxSize;

    /**
     * Ctor.
     *
     * @param minSize Minimal size of merged (accumulated) ByteBuffer.
     * @param maxSize Maximum size of merged (accumulated) ByteBuffer.
     */
    public Merging(final int minSize, final int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    /**
     * Merge Flowable ByteBuffer objects and produce ByteBuffer objects with target size.
     * @param source Source of data blocks. Must be sequential source.
     * @return Flowable with merged blocks.
     */
    public Flowable<ByteBuffer> mergeFlow(final Flowable<ByteBuffer> source) {
        final MergeState state = new MergeState(maxSize);
        return source.concatMap(chunk -> {
            if (chunk.remaining() > maxSize) {
                throw new ArtipieIOException("Input chunk is bigger than maximum size");
            }
            final int diff = Math.min(chunk.remaining(), maxSize - state.getAccumulated());
            state.add(chunk, diff);
            chunk.position(chunk.position() + diff);
            if (state.getAccumulated() < this.minSize) {
                return Flowable.empty();
            }
            final ByteBuffer payload = state.wrapAccumulated();
            state.reset(maxSize);
            final int remaining = chunk.remaining();
            state.add(chunk, remaining);
            return Flowable.just(payload);
        }).concatWith(Maybe.defer(() -> {
            if (state.getAccumulated() == 0) {
                return Maybe.empty();
            }
            return Maybe.just(state.wrapAccumulated());
        }));
    }

    /**
     * Current state of the flow merging.
     */
    private class MergeState {

        /**
         * Data accumulator array.
         */
        private byte[] accumulator;

        /**
         * Count of bytes accumulated.
         */
        private int accumulated;

        /**
         * Ctor.
         *
         * @param size Accumulator size. Maximum size of data accumulated.
         */
        MergeState(final int size) {
            this.accumulator = new byte[size];
        }

        /**
         * Returns amount of bytes currently accumulated.
         *
         * @return Count of bytes accumulated.
         */
        public int getAccumulated() {
            return accumulated;
        }

        /**
         * Resets `accumulated` and creates new accumulator array.
         *
         * @param size Accumulator size. Maximum size of data accumulated.
         */
        public void reset(final int size) {
            this.accumulator = new byte[size];
            this.accumulated = 0;
        }

        /**
         * Wrap accumulated data array in ByteBuffer.
         *
         * @return ByteBuffer instance backed by accumulator array. See `reset()` to change backing array.
         */
        public ByteBuffer wrapAccumulated() {
            return ByteBuffer.wrap(this.accumulator, 0, this.accumulated);
        }

        /**
         * Add `length` bytes from `chunk` to the accumulator array.
         *
         * @param chunk ByteBuffer with data.
         * @param count Amount of bytes to accumulate from `chunk`.
         */
        public void add(final ByteBuffer chunk, final int count) {
            chunk.get(chunk.position(), this.accumulator, this.accumulated, count);
            this.accumulated += count;
        }
    }
}
