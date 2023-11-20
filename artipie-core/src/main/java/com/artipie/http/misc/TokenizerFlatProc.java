/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Byte buffer publisher processor tokenizer as a flat publisher of byte buffers.
 *
 * @since 1.0
 */
public final class TokenizerFlatProc implements Processor<ByteBuffer, ByteBuffer>,
    ByteBufferTokenizer.Receiver {

    /**
     * Initial buffer capacity.
     */
    private static final int CAP_BUF = 128;

    /**
     * Tokenizer.
     */
    private final ByteBufferTokenizer tokenizer;

    /**
     * Buffer accumulator.
     */
    private final BufAccumulator accumulator;

    /**
     * Completed flag.
     */
    private final AtomicBoolean completed;

    /**
     * Subscription lock.
     */
    private final Object lock;

    /**
     * Downstream subscriber.
     */
    private volatile Subscriber<? super ByteBuffer> downstream;

    /**
     * Upstream proxy.
     */
    private volatile ProxySubscription upstream;

    /**
     * New tokenizer with default capacity.
     * @param delim Delimiter
     */
    public TokenizerFlatProc(final String delim) {
        this(delim, TokenizerFlatProc.CAP_BUF);
    }

    /**
     * New tokenizer processor.
     * @param delim Delimiter token
     * @param cap Buffer capacity in bytes
     */
    public TokenizerFlatProc(final String delim, final int cap) {
        this.tokenizer = new ByteBufferTokenizer(this, delim.getBytes(StandardCharsets.US_ASCII));
        this.accumulator = new BufAccumulator(cap);
        this.completed = new AtomicBoolean();
        this.lock = new Object();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> sub) {
        synchronized (this.lock) {
            if (this.downstream == null) {
                this.downstream = sub;
            } else {
                sub.onSubscribe(DummySubscription.VALUE);
                sub.onError(new IllegalStateException("Only one downstream supported"));
                return;
            }
            if (this.upstream != null) {
                this.downstream.onSubscribe(this.upstream);
            }
        }
    }

    @Override
    public void onSubscribe(final Subscription sub) {
        synchronized (this.lock) {
            if (this.upstream != null) {
                throw new IllegalStateException("Already subscribed");
            }
            this.upstream = new ProxySubscription(sub);
            if (this.downstream != null) {
                this.downstream.onSubscribe(this.upstream);
            }
        }
    }

    @Override
    public void onNext(final ByteBuffer buffer) {
        this.tokenizer.push(buffer);
    }

    @Override
    public void onError(final Throwable err) {
        this.downstream.onError(err);
    }

    @Override
    public void onComplete() {
        if (this.completed.compareAndSet(false, true)) {
            this.tokenizer.close();
        }
    }

    @Override
    public void receive(final ByteBuffer next, final boolean end) {
        this.upstream.receive();
        this.accumulator.write(next);
        if (end) {
            final ByteBuffer dst = ByteBuffer.allocate(this.accumulator.size());
            this.accumulator.read(dst);
            dst.flip();
            this.downstream.onNext(dst);
            if (this.completed.get()) {
                this.downstream.onComplete();
                this.accumulator.close();
            }
        }
    }

    /**
     * Upstream subscription proxy.
     * <p>
     * It handle requests from downstream and translate it to upstream requests
     * depends on about of processed items.
     * </p>
     * @since 1.0
     * @todo #299:30min Implement this subscription correctly.
     *  Now it requests MAX_VALUE from upstream on first request from downstream.
     *  It should count requests from downstream and request upstream only on demand.
     */
    private static final class ProxySubscription implements Subscription {

        /**
         * Upstream subscription.
         */
        private final Subscription upstream;

        /**
         * Requested flag.
         */
        private final AtomicBoolean requested;

        /**
         * New proxy for upstream.
         * @param upstream Subscription
         */
        ProxySubscription(final Subscription upstream) {
            this.upstream = upstream;
            this.requested = new AtomicBoolean();
        }

        @Override
        public void request(final long amount) {
            if (this.requested.compareAndSet(false, true)) {
                this.upstream.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void cancel() {
            this.upstream.cancel();
        }

        /**
         * Notify item received.
         * @checkstyle NonStaticMethodCheck (10 lines)
         */
        public void receive() {
            // not implemented
        }
    }
}
