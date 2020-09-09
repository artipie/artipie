/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;

/**
 * Slice which cancel requests on timeout.
 * <p>
 * If response is not sent in fixed time-span,
 * this slice will respond with {@code 503} status code.
 * </p>
 * @since 0.10
 */
public final class TimeoutSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Timeout duration.
     */
    private final Duration duration;

    /**
     * Executor service.
     */
    private final ScheduledExecutorService exec;

    /**
     * Wrpap {@link Slice} implementation.
     * @param origin Origin slice
     * @param duration Timeout duration
     */
    public TimeoutSlice(final Slice origin, final Duration duration) {
        this(
            origin,
            duration,
            Executors.newSingleThreadScheduledExecutor(
                run -> new Thread(run, TimeoutSlice.class.getSimpleName())
            )
        );
    }

    /**
     * Wrpap {@link Slice} implementation with specified scheduler.
     * @param origin Origin slice
     * @param duration Timeout duration
     * @param exec Scheduler
     */
    public TimeoutSlice(final Slice origin, final Duration duration,
        final ScheduledExecutorService exec) {
        this.origin = origin;
        this.duration = duration;
        this.exec = exec;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new TimeoutResponse(
            this.origin.response(line, headers, body), this.duration, this.exec
        );
    }

    /**
     * Response with timeout support.
     * @since 0.10
     */
    private static final class TimeoutResponse implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Timeout duration.
         */
        private final Duration duration;

        /**
         * Scheduler.
         */
        private final ScheduledExecutorService exec;

        /**
         * Ctor.
         * @param origin Origin response
         * @param duration Timeout duration
         * @param exec Executor service
         */
        TimeoutResponse(final Response origin, final Duration duration,
            final ScheduledExecutorService exec) {
            this.origin = origin;
            this.duration = duration;
            this.exec = exec;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            final TimeoutConnection tcon = new TimeoutConnection(connection, this.exec);
            return CompletableFuture.anyOf(
                this.origin.send(tcon).toCompletableFuture(),
                tcon.timeout(this.duration)
            ).thenApply(ignore -> null);
        }
    }

    /**
     * Connection with timeout support.
     * @since 0.10
     */
    private static final class TimeoutConnection implements Connection {

        /**
         * Origin connection.
         */
        private final Connection origin;

        /**
         * Scheduler.
         */
        private final ScheduledExecutorService exec;

        /**
         * Completed flag.
         */
        private final AtomicBoolean completed;

        /**
         * Ctor.
         * @param origin Origin connection
         * @param exec Scheduler
         */
        TimeoutConnection(final Connection origin, final ScheduledExecutorService exec) {
            this.origin = origin;
            this.exec = exec;
            this.completed = new AtomicBoolean();
        }

        @Override
        public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
            final Publisher<ByteBuffer> body) {
            final CompletionStage<Void> res;
            if (status == RsStatus.CONTINUE || this.completed.compareAndSet(false, true)) {
                res = this.origin.accept(status, headers, body);
            } else {
                res = CompletableFuture.completedFuture(null);
            }
            return res;
        }

        /**
         * Start timeout task.
         * @param duration Timeout duration
         * @return Task future
         */
        CompletableFuture<?> timeout(final Duration duration) {
            final CompletableFuture<?> future = new CompletableFuture<>();
            this.exec.schedule(
                () -> {
                    if (this.completed.compareAndSet(false, true)) {
                        this.origin.accept(
                            RsStatus.UNAVAILABLE, Headers.EMPTY,
                            new Content.From(
                                "Request cancelled by timeout".getBytes(StandardCharsets.UTF_8)
                            )
                        );
                        future.complete(null);
                    }
                },
                duration.toMillis(), TimeUnit.MILLISECONDS
            );
            return future;
        }
    }
}
