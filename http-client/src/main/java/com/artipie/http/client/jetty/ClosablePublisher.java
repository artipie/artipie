/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import hu.akarnokd.rxjava2.interop.MaybeInterop;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.eclipse.jetty.io.Content;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Publisher that subscribes and consumes origin publisher if it was not done yet.
 *
 * @since 0.1
 */
final class ClosablePublisher implements Publisher<Content.Chunk> {

    /**
     * Origin publisher.
     */
    private final Publisher<Content.Chunk> origin;

    /**
     * Subscribed flag.
     */
    private volatile boolean subscribed;

    /**
     * Ctor.
     *
     * @param origin Origin publisher.
     */
    ClosablePublisher(final Publisher<Content.Chunk> origin) {
        this.origin = origin;
    }

    @Override
    public void subscribe(final Subscriber<? super Content.Chunk> subscriber) {
        this.subscribed = true;
        this.origin.subscribe(subscriber);
    }

    /**
     * Closes publisher.
     *
     * @return Completion of publisher closing.
     */
    public CompletionStage<Void> close() {
        final CompletionStage<Void> result;
        if (this.subscribed) {
            result = CompletableFuture.allOf();
        } else {
            result = Flowable.fromPublisher(this.origin)
                .lastElement()
                .to(MaybeInterop.get())
                .thenCompose(ignored -> CompletableFuture.allOf());
        }
        return result;
    }
}
