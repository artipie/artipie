/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.http.Headers;
import com.artipie.http.misc.BufAccumulator;
import com.artipie.http.misc.ByteBufferTokenizer;
import com.artipie.http.misc.DummySubscription;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import net.jcip.annotations.GuardedBy;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Multipart request part.
 * @since 1.0
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 */
@SuppressWarnings("PMD.NullAssignment")
final class MultiPart implements RqMultipart.Part, ByteBufferTokenizer.Receiver, Subscription {

    /**
     * Header buffer capacity.
     */
    private static final int CAP_HEADER = 256;

    /**
     * Part body buffer capacity.
     */
    private static final int CAP_PART = 1024;

    /**
     * Delimiter token.
     */
    private static final String DELIM = "\r\n\r\n";

    /**
     * CRLF tokenizer.
     */
    @GuardedBy("lock")
    private final ByteBufferTokenizer tokenizer;

    /**
     * Part processor executor.
     */
    private final ExecutorService exec;

    /**
     * Multipart header.
     */
    private final MultipartHeaders hdr;

    /**
     * Downstream.
     */
    private volatile Subscriber<? super ByteBuffer> downstream;

    /**
     * Head processed.
     */
    private volatile boolean head;

    /**
     * Ready callback.
     * <p>
     * Called when the header is received and the part could be submitted to
     * {@link RqMultipart.Part} downstream.
     * </p>
     */
    private final Consumer<? super RqMultipart.Part> ready;

    /**
     * Temporary body accumulator.
     * <p>
     * It's needed when the downstream connected after the part of body received.
     * It may happen if the first chunk of body received with last header chunk
     * before downstream subscription.
     * </p>
     */
    @GuardedBy("lock")
    private final BufAccumulator tmpacc;

    /**
     * Completed flag.
     */
    private volatile boolean completed;

    /**
     * Completion handler.
     */
    private final Completion<?> completion;

    /**
     * State synchronization.
     */
    private final Object lock;

    /**
     * Downstream demand counter.
     */
    private volatile long demand;

    /**
     * New multipart request part.
     * @param completion Upstream completion handler
     * @param ready Ready callback
     * @param exec Executor service
     */
    MultiPart(final Completion<?> completion, final Consumer<? super RqMultipart.Part> ready,
        final ExecutorService exec) {
        this.ready = ready;
        this.completion = completion;
        this.tokenizer = new ByteBufferTokenizer(
            this, MultiPart.DELIM.getBytes(), MultiPart.CAP_PART
        );
        this.hdr = new MultipartHeaders(MultiPart.CAP_HEADER);
        this.tmpacc = new BufAccumulator(MultiPart.CAP_HEADER);
        this.lock = new Object();
        this.exec = exec;
    }

    @Override
    public Headers headers() {
        return this.hdr;
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> sub) {
        synchronized (this.lock) {
            if (this.downstream != null) {
                sub.onSubscribe(DummySubscription.VALUE);
                sub.onError(new IllegalStateException("Downstream already connected"));
                return;
            }
            this.downstream = sub;
            sub.onSubscribe(this);
        }
    }

    @Override
    public void receive(final ByteBuffer next, final boolean end) {
        synchronized (this.lock) {
            if (this.head) {
                this.nextChunk(next);
            } else {
                this.hdr.push(next);
                if (end) {
                    this.head = true;
                    this.ready.accept(this);
                }
            }
        }
    }

    @Override
    public void request(final long amt) {
        if (amt <= 0) {
            throw new IllegalStateException("Requested amount should be greater than zero");
        }
        if (this.downstream == null) {
            return;
        }
        synchronized (this.lock) {
            if (amt == Long.MAX_VALUE || this.demand == Long.MAX_VALUE || amt + this.demand < 0) {
                this.demand = Long.MAX_VALUE;
            } else {
                this.demand += amt;
            }
        }
        this.exec.submit(this::deliver);
    }

    @Override
    public void cancel() {
        synchronized (this.lock) {
            this.downstream = null;
        }
    }

    /**
     * Push next chunk of raw data.
     * @param chunk Chunk buffer
     */
    void push(final ByteBuffer chunk) {
        synchronized (this.lock) {
            if (this.head) {
                this.nextChunk(chunk);
            } else {
                this.tokenizer.push(chunk);
                // head flag could be changed to true after processing chunk
                if (this.head) {
                    this.tokenizer.close();
                }
            }
        }
    }

    /**
     * Flush all data in temporary buffers.
     */
    void flush() {
        synchronized (this.lock) {
            if (!this.head) {
                this.tokenizer.close();
            }
            this.completed = true;
            this.exec.submit(this::deliver);
        }
    }

    /**
     * Process next chunk of body data.
     * @param next Next buffer
     */
    private void nextChunk(final ByteBuffer next) {
        this.tmpacc.write(next);
        if (this.downstream != null) {
            this.exec.submit(this::deliver);
        }
    }

    /**
     * Deliver accumulated data to downstream.
     */
    private void deliver() {
        synchronized (this.lock) {
            boolean delivered = false;
            while (this.demand > 0) {
                final ByteBuffer out = ByteBuffer.allocate(4096);
                if (this.tmpacc.read(out) < 0) {
                    break;
                }
                out.flip();
                this.downstream.onNext(out);
                delivered = true;
                if (this.demand != Long.MAX_VALUE) {
                    --this.demand;
                }
            }
            if (this.completed && this.tmpacc.empty()) {
                if (!delivered) {
                    this.downstream.onNext(ByteBuffer.allocate(0));
                }
                this.tmpacc.close();
                this.downstream.onComplete();
                this.downstream = null;
                this.completion.itemCompleted();
            }
        }
    }
}
