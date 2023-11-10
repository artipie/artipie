/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.reactivestreams.Subscriber;

/**
 * Content wrapper that allows to get byte buffers count and size of content’s data.
 * @since 0.28.0
 */
public final class ChunksAndSizeMetricsContent implements Content {
    /**
     * Original content.
     */
    private final Content original;

    /**
     * Callback consumer.
     * The first attribute is chunks count, the second is size of received data.
     */
    private final BiConsumer<Integer, Long> callback;

    /**
     * Ctor.
     * The first attribute of callback consumer is chunks count,
     * the second is size of received data.
     *
     * @param original Original Content.
     * @param callback Callback consumer.
     */
    public ChunksAndSizeMetricsContent(
        final Content original,
        final BiConsumer<Integer, Long> callback) {
        this.original = original;
        this.callback = callback;
    }

    @Override
    public Optional<Long> size() {
        return this.original.size();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.original.subscribe(
            new ChunksAndSizeSubscriber(subscriber, this.callback)
        );
    }
}
