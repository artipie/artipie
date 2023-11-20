/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.group;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Response result.
 * <p>
 * The result of {@link GroupResponse}, it's waiting in order for all previous responses
 * to be completed, and may be replied to connection or cancelled.
 * </p>
 * @since 0.11
 */
final class GroupResult {

    /**
     * Subscriber which cancel publisher subscription.
     * @checkstyle AnonInnerLengthCheck (25 lines)
     */
    private static final Subscriber<? super Object> CANCEL_SUB = new Subscriber<Object>() {
        @Override
        public void onSubscribe(final Subscription sub) {
            sub.cancel();
        }

        @Override
        public void onNext(final Object obj) {
            // nothing to do
        }

        @Override
        public void onError(final Throwable err) {
            // nothing to do
        }

        @Override
        public void onComplete() {
            // nothing to do
        }
    };

    /**
     * Response status.
     */
    private final RsStatus status;

    /**
     * Response headers.
     */
    private final Headers headers;

    /**
     * Body publisher.
     */
    private final Publisher<ByteBuffer> body;

    /**
     * Completed flag.
     */
    private final AtomicBoolean completed;

    /**
     * New response result.
     * @param status Response status
     * @param headers Response headers
     * @param body Body publisher
     */
    GroupResult(final RsStatus status, final Headers headers,
        final Publisher<ByteBuffer> body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
        this.completed = new AtomicBoolean();
    }

    /**
     * Replay response to connection.
     * @param con Connection
     * @return Future
     */
    public CompletionStage<Void> replay(final Connection con) {
        final CompletionStage<Void> res;
        if (this.completed.compareAndSet(false, true)) {
            res = con.accept(this.status, this.headers, this.body);
        } else {
            res = CompletableFuture.completedFuture(null);
        }
        return res;
    }

    /**
     * Check if response was successes.
     * @return True if success
     */
    public boolean success() {
        final int code = Integer.parseInt(this.status.code());
        // @checkstyle MagicNumberCheck (1 line)
        return code >= 200 && code < 300;
    }

    /**
     * Cancel response body stream.
     */
    void cancel() {
        if (this.completed.compareAndSet(false, true)) {
            this.body.subscribe(GroupResult.CANCEL_SUB);
        }
    }
}
