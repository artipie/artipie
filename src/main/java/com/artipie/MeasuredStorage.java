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
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Storage implementation which measures IO operations execution time.
 * @since 0.10
 * @checkstyle AnonInnerLengthCheck (500 lines)
 */
public final class MeasuredStorage implements Storage {

    /**
     * Origin storage.
     */
    private final Storage origin;

    /**
     * Wraps storage with measured decorator.
     * @param storage Origin storage to measure
     */
    public MeasuredStorage(final Storage storage) {
        this.origin = storage;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        final long start = System.nanoTime();
        return this.origin.exists(key).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class, "exists(%s): %s", key.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key key) {
        final long start = System.nanoTime();
        return this.origin.list(key).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class, "list(%s): %s", key.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final long start = System.nanoTime();
        return this.origin.save(
            key,
            new Content() {
                @Override
                public Optional<Long> size() {
                    return content.size();
                }

                @Override
                public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
                    content.subscribe(
                        new Subscriber<>() {
                            @Override
                            public void onSubscribe(final Subscription subscription) {
                                Logger.debug(
                                    MeasuredStorage.class,
                                    "save(%s): onSubscribe(subscription=%s) %s",
                                    key.string(), subscription, millisMessage(start)
                                );
                                subscriber.onSubscribe(
                                    new Subscription() {
                                        @Override
                                        public void request(final long num) {
                                            Logger.debug(
                                                MeasuredStorage.class,
                                                "save(%s): request(n=%d) %s",
                                                key.string(), num, millisMessage(start)
                                            );
                                            subscription.request(num);
                                        }

                                        @Override
                                        public void cancel() {
                                            Logger.debug(
                                                MeasuredStorage.class,
                                                "save(%s): cancel() %s",
                                                key.string(), millisMessage(start)
                                            );
                                            subscription.cancel();
                                        }
                                    }
                                );
                            }

                            @Override
                            public void onNext(final ByteBuffer buffer) {
                                Logger.debug(
                                    MeasuredStorage.class,
                                    "save(%s): next(buffer=%s) %s",
                                    key.string(), buffer, millisMessage(start)
                                );
                                subscriber.onNext(buffer);
                            }

                            @Override
                            public void onError(final Throwable err) {
                                Logger.debug(
                                    MeasuredStorage.class,
                                    "save(%s): onError(subscription=%s) %s",
                                    key.string(), err, millisMessage(start)
                                );
                                subscriber.onError(err);
                            }

                            @Override
                            public void onComplete() {
                                Logger.debug(
                                    MeasuredStorage.class,
                                    "save(%s): onComplete() %s",
                                    key.string(), millisMessage(start)
                                );
                                subscriber.onComplete();
                            }
                        }
                    );
                }
            }
        ).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class,
                    "save(%s): %s",
                    key.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key src, final Key dst) {
        final long start = System.nanoTime();
        return this.origin.move(src, dst).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class,
                    "move(%s, %s): %s",
                    src.string(), dst.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        final long start = System.nanoTime();
        return this.origin.size(key).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class, "size(%s): %s", key.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final long start = System.nanoTime();
        return this.origin.value(key).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class,
                    "value(%s): %s", key.string(), millisMessage(start)
                );
                return new Content() {
                    @Override
                    public Optional<Long> size() {
                        return res.size();
                    }

                    @Override
                    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
                        res.subscribe(
                            new Subscriber<>() {
                                @Override
                                public void onSubscribe(final Subscription subscription) {
                                    Logger.debug(
                                        MeasuredStorage.class,
                                        "value(%s): onSubscribe(%s) %s",
                                        key.string(), subscription, millisMessage(start)
                                    );
                                    subscriber.onSubscribe(subscription);
                                }

                                @Override
                                public void onNext(final ByteBuffer buffer) {
                                    Logger.debug(
                                        MeasuredStorage.class,
                                        "value(%s): onNext(%s) %s",
                                        key.string(), buffer, millisMessage(start)
                                    );
                                    subscriber.onNext(buffer);
                                }

                                @Override
                                public void onError(final Throwable err) {
                                    Logger.debug(
                                        MeasuredStorage.class,
                                        "value(%s): error(%s) %s",
                                        key.string(), err, millisMessage(start)
                                    );
                                    subscriber.onError(err);
                                }

                                @Override
                                public void onComplete() {
                                    Logger.debug(
                                        MeasuredStorage.class,
                                        "value(%s): complete() %s",
                                        key.string(), millisMessage(start)
                                    );
                                    subscriber.onComplete();
                                }
                            }
                        );
                    }
                };
            }
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        final long start = System.nanoTime();
        return this.origin.delete(key).thenApply(
            res -> {
                Logger.debug(
                    MeasuredStorage.class,
                    "delete(%s): %s", key.string(), millisMessage(start)
                );
                return res;
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> function) {
        return this.origin.exclusively(key, function);
    }

    /**
     * Amount of milliseconds from time.
     * @param from Starting point in nanoseconds
     * @return Formatted milliseconds message
     */
    private static String millisMessage(final long from) {
        // @checkstyle MagicNumberCheck (1 line)
        return String.format("%dms", (System.nanoTime() - from) / 1_000_000);
    }
}
