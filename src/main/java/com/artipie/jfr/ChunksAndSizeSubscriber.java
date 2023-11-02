/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Subscriber wrapper that allows to get byte buffers count and size of received data.
 *
 * @since 0.28.0
 */
public final class ChunksAndSizeSubscriber implements Subscriber<ByteBuffer> {

    /**
     * Original subscriber.
     */
    private final Subscriber<? super ByteBuffer> original;

    /**
     * Callback consumer.
     * The first attribute is chunks count, the second is size of received data.
     */
    private final BiConsumer<Integer, Long> callback;

    /**
     * Chunks counter.
     */
    private final AtomicInteger chunks;

    /**
     * Size of received data.
     */
    private final AtomicLong received;

    /**
     * Ctor.
     * The first attribute of callback consumer is chunks count,
     * the second is size of received data.
     *
     * @param original Original subscriber.
     * @param callback Callback consumer.
     */
    public ChunksAndSizeSubscriber(final Subscriber<? super ByteBuffer> original,
        final BiConsumer<Integer, Long> callback) {
        this.original = original;
        this.callback = callback;
        this.chunks = new AtomicInteger(0);
        this.received = new AtomicLong(0);
    }

    @Override
    public void onSubscribe(final Subscription sub) {
        this.original.onSubscribe(
            new Subscription() {
                @Override
                public void request(final long num) {
                    sub.request(num);
                }

                @Override
                public void cancel() {
                    sub.cancel();
                }
            }
        );
    }

    @Override
    public void onNext(final ByteBuffer buffer) {
        this.chunks.incrementAndGet();
        this.received.addAndGet(buffer.remaining());
        this.original.onNext(buffer);
    }

    @Override
    public void onError(final Throwable err) {
        this.callback.accept(-1, 0L);
        this.original.onError(err);
    }

    @Override
    public void onComplete() {
        this.callback.accept(
            this.chunks.get(),
            this.received.get()
        );
        this.original.onComplete();
    }
}
