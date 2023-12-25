/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A publish which can be consumed only once.
 * @param <T> The type of publisher elements.
 * @since 0.23
 */
@SuppressWarnings("PMD.UncommentedEmptyMethodBody")
public final class OneTimePublisher<T> implements Publisher<T> {

    /**
     * The original publisher.
     */
    private final Publisher<T> original;

    /**
     * The amount of subscribers.
     */
    private final AtomicInteger subscribers;

    /**
     * Wrap a publish in a way it can be used only once.
     * @param original The original publisher.
     */
    public OneTimePublisher(final Publisher<T> original) {
        this.original = original;
        this.subscribers = new AtomicInteger(0);
    }

    @Override
    public void subscribe(final Subscriber<? super T> sub) {
        final int subs = this.subscribers.incrementAndGet();
        if (subs == 1) {
            this.original.subscribe(sub);
        } else {
            final String msg =
                "The subscriber could not be consumed more than once. Failed on #%d attempt";
            sub.onSubscribe(
                new Subscription() {
                    @Override
                    public void request(final long cnt) {
                    }

                    @Override
                    public void cancel() {
                    }
                }
            );
            sub.onError(new ArtipieIOException(String.format(msg, subs)));
        }
    }
}
