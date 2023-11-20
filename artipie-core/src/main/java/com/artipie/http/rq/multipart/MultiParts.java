/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.ArtipieException;
import com.artipie.http.misc.ByteBufferTokenizer;
import com.artipie.http.misc.Pipeline;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Multipart parts publisher.
 *
 * @since 1.0
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 */
final class MultiParts implements Processor<ByteBuffer, RqMultipart.Part>,
    ByteBufferTokenizer.Receiver {

    /**
     * Cached thread pool for parts processing.
     */
    private static final ExecutorService CACHED_PEXEC = Executors.newCachedThreadPool();

    /**
     * Upstream downstream pipeline.
     */
    private final Pipeline<RqMultipart.Part> pipeline;

    /**
     * Parts tokenizer.
     */
    private final ByteBufferTokenizer tokenizer;

    /**
     * Subscription executor service.
     */
    private final ExecutorService exec;

    /**
     * Part executor service.
     */
    private final ExecutorService pexec;

    /**
     * State synchronization.
     */
    private final Object lock;

    /**
     * Current part.
     */
    private volatile MultiPart current;

    /**
     * State flags.
     */
    private final State state;

    /**
     * Completion handler.
     */
    private final Completion<?> completion;

    /**
     * New multipart parts publisher for upstream publisher.
     * @param boundary Boundary token delimiter of parts
     */
    MultiParts(final String boundary) {
        this(boundary, MultiParts.CACHED_PEXEC);
    }

    /**
     * New multipart parts publisher for upstream publisher.
     * @param boundary Boundary token delimiter of parts
     * @param pexec Parts processing executor
     */
    MultiParts(final String boundary, final ExecutorService pexec) {
        this.tokenizer = new ByteBufferTokenizer(
            this, boundary.getBytes(StandardCharsets.US_ASCII)
        );
        this.exec = Executors.newSingleThreadExecutor();
        this.pipeline = new Pipeline<>();
        this.completion = new Completion<>(this.pipeline);
        this.state = new State();
        this.lock = new Object();
        this.pexec = pexec;
    }

    /**
     * Subscribe publisher to this processor asynchronously.
     * @param pub Upstream publisher
     */
    public void subscribeAsync(final Publisher<ByteBuffer> pub) {
        this.exec.submit(() -> pub.subscribe(this));
    }

    @Override
    public void subscribe(final Subscriber<? super RqMultipart.Part> sub) {
        this.pipeline.connect(sub);
    }

    @Override
    public void onSubscribe(final Subscription sub) {
        this.pipeline.onSubscribe(sub);
    }

    @Override
    public void onNext(final ByteBuffer chunk) {
        final ByteBuffer next;
        if (this.state.isInit()) {
            // multipart preamble is tricky:
            // if request is started with boundary, then it donesn't have a preamble
            // but we're splitting it by \r\n<boundary> token.
            // To tell tokenizer emmit empty chunk on non-preamble first buffer started with
            // boudnary we need to add \r\n to it.
            next = ByteBuffer.allocate(chunk.limit() + 2);
            next.put("\r\n".getBytes(StandardCharsets.US_ASCII));
            next.put(chunk);
            next.rewind();
        } else {
            next = chunk;
        }
        this.tokenizer.push(next);
        this.pipeline.request(1L);
    }

    @Override
    public void onError(final Throwable err) {
        this.pipeline.onError(new ArtipieException("Upstream failed", err));
        this.exec.shutdown();
    }

    @Override
    public void onComplete() {
        this.completion.upstreamCompleted();
    }

    @Override
    public void receive(final ByteBuffer next, final boolean end) {
        synchronized (this.lock) {
            this.state.patch(next, end);
            if (this.state.shouldIgnore()) {
                return;
            }
            if (this.state.started()) {
                this.completion.itemStarted();
                this.current = new MultiPart(
                    this.completion,
                    part -> this.exec.submit(() -> this.pipeline.onNext(part)),
                    this.pexec
                );
            }
            this.current.push(next);
            if (this.state.ended()) {
                this.current.flush();
            }
        }
    }
}
