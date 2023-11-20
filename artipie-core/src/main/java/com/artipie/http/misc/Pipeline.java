/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Async pipeline for flow processor to connect upstream subscriber and downstream susbscription.
 * @param <D> Downstream type
 * @since 1.0
 */
@SuppressWarnings("PMD.NullAssignment")
public final class Pipeline<D> implements Subscriber<D>, Subscription {

    /**
     * Synchronization object.
     */
    private final Object lock;

    /**
     * Downstream subscriber.
     */
    private volatile Subscriber<? super D> downstream;

    /**
     * Upstream Subscription.
     */
    private volatile Subscription upstream;

    /**
     * Completed cache.
     */
    private volatile boolean completed;

    /**
     * Error cache.
     */
    private volatile Throwable error;

    /**
     * New pipeline.
     */
    public Pipeline() {
        this.lock = new Object();
    }

    /**
     * Connect downstream.
     * @param sub Downstream subscriber
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    public void connect(final Subscriber<? super D> sub) {
        synchronized (this.lock) {
            if (this.downstream != null) {
                sub.onSubscribe(DummySubscription.VALUE);
                sub.onError(new IllegalStateException("Downstream already connected"));
                return;
            }
            if (this.completed && this.error == null) {
                sub.onSubscribe(DummySubscription.VALUE);
                sub.onComplete();
            } else if (this.error != null) {
                sub.onSubscribe(DummySubscription.VALUE);
                sub.onError(this.error);
            } else {
                this.downstream = sub;
                this.checkRequest();
            }
        }
    }

    @Override
    public void onComplete() {
        synchronized (this.lock) {
            if (this.downstream == null) {
                this.completed = true;
            } else {
                this.downstream.onComplete();
            }
            this.cleanup();
        }
    }

    @Override
    public void onError(final Throwable err) {
        synchronized (this.lock) {
            if (this.downstream == null) {
                this.completed = true;
                this.error = err;
            } else {
                this.downstream.onError(err);
            }
            this.cleanup();
        }
    }

    @Override
    public void onNext(final D item) {
        synchronized (this.lock) {
            assert this.downstream != null;
            this.downstream.onNext(item);
        }
    }

    @Override
    public void onSubscribe(final Subscription sub) {
        synchronized (this.lock) {
            if (this.upstream != null) {
                throw new IllegalStateException("Can't subscribe twice");
            }
            this.upstream = sub;
            this.checkRequest();
        }
    }

    @Override
    public void cancel() {
        synchronized (this.lock) {
            this.cleanup();
        }
    }

    @Override
    public void request(final long amt) {
        assert this.downstream != null && this.upstream != null;
        this.upstream.request(amt);
    }

    /**
     * Check if all required parts are connected, and request from upstream if so.
     * @checkstyle MethodBodyCommentsCheck (10 lines)
     */
    private void checkRequest() {
        synchronized (this.lock) {
            if (this.downstream != null && this.upstream != null) {
                this.downstream.onSubscribe(this);
                // upstream can be null here if downstream and upstream are
                // synchronous and it's completed after onSubscribe call
                if (this.upstream != null) {
                    this.upstream.request(1L);
                }
            }
        }
    }

    /**
     * According to reactive-stream specification, should clear upstream
     * reference and optionally cancel the upstream.
     */
    private void cleanup() {
        if (this.upstream != null) {
            this.upstream.cancel();
        }
        this.upstream = null;
        this.downstream = null;
    }
}
