/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.nio.ByteBuffer;

/**
 * Merges ByteBuffer objects to bigger ones, according to specified `size`.
 * Produces new ByteBuffer blocks according to specified target size.
 *
 * @since v0.30.12
 */
public class Merging {

    /**
     * Data accumulator array.
     */
    byte[] accumulator;

    /**
     * Count of bytes accumulated.
     */
    int accumulated;

    /**
     * Target block size.
     */
    final int size;

    /**
     * Ctor.
     *
     * @param size Size of target merged (accumulated) ByteBuffer.
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
            if (remaining > accumulator.length - accumulated) {
                throw new ArtipieIOException("Splitting error. Chunk is too big.");
            }
            int j = accumulated;
            for (int i = chunk.position(); i < chunk.limit(); ++i) {
                accumulator[j++] = chunk.get(i);
            }
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
