/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

/**
 * Content wrapper that allows to get byte buffers count and size of content’s data.
 */
public final class ChunksAndSizeMetricsPublisher implements Publisher<ByteBuffer> {
    /**
     * Original publisher.
     */
    private final Content original;

    /**
     * Callback consumer.
     * The first attribute is chunks count, the second is size of received data.
     */
    private final BiConsumer<Integer, Long> callback;

    /**
     * The first attribute of callback consumer is chunks count,
     * the second is size of received data.
     *
     * @param original Original publisher.
     * @param callback Callback consumer.
     */
    public ChunksAndSizeMetricsPublisher(Content original, BiConsumer<Integer, Long> callback) {
        this.original = original;
        this.callback = callback;
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.original.subscribe(
            new ChunksAndSizeSubscriber(subscriber, this.callback)
        );
    }
}
