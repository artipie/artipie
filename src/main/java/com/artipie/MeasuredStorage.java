/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.jcabi.log.Logger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Storage implementation which measures IO operations execution time.
 * @since 0.10
 * @checkstyle AnonInnerLengthCheck (500 lines)
 */
@SuppressWarnings({"deprecation", "PMD.TooManyMethods"})
public final class MeasuredStorage implements Storage {

    /**
     * Origin storage.
     */
    private final Storage origin;

    /**
     * Log level.
     */
    private final Level level;

    /**
     * Wraps storage with measured decorator.
     * @param storage Origin storage to measure
     */
    public MeasuredStorage(final Storage storage) {
        this(storage, Level.FINEST);
    }

    /**
     * Wraps storage with measured decorator.
     * @param storage Origin storage to measure
     * @param level Log level
     */
    public MeasuredStorage(final Storage storage, final Level level) {
        this.origin = storage;
        this.level = level;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        final long start = System.nanoTime();
        return this.origin.exists(key).thenApply(
            res -> {
                this.log("exists(%s): %s", key.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key key) {
        final long start = System.nanoTime();
        return this.origin.list(key).thenApply(
            res -> {
                this.log("list(%s): %s", key.string(), millisMessage(start));
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
                                MeasuredStorage.this.log(
                                    "save(%s): onSubscribe(subscription=%s) %s",
                                    key.string(), subscription, millisMessage(start)
                                );
                                subscriber.onSubscribe(
                                    new Subscription() {
                                        @Override
                                        public void request(final long num) {
                                            MeasuredStorage.this.log(
                                                "save(%s): request(n=%d) %s",
                                                key.string(), num, millisMessage(start)
                                            );
                                            subscription.request(num);
                                        }

                                        @Override
                                        public void cancel() {
                                            MeasuredStorage.this.log(
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
                                MeasuredStorage.this.log(
                                    "save(%s): next(buffer=%s) %s",
                                    key.string(), buffer, millisMessage(start)
                                );
                                subscriber.onNext(buffer);
                            }

                            @Override
                            public void onError(final Throwable err) {
                                MeasuredStorage.this.log(
                                    "save(%s): onError(subscription=%s) %s",
                                    key.string(), err, millisMessage(start)
                                );
                                subscriber.onError(err);
                            }

                            @Override
                            public void onComplete() {
                                MeasuredStorage.this.log(
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
                this.log("save(%s): %s", key.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key src, final Key dst) {
        final long start = System.nanoTime();
        return this.origin.move(src, dst).thenApply(
            res -> {
                this.log("move(%s, %s): %s", src.string(), dst.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Long> size(final Key key) {
        final long start = System.nanoTime();
        return this.origin.size(key).thenApply(
            res -> {
                this.log("size(%s): %s", key.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        final long start = System.nanoTime();
        return this.origin.metadata(key).thenApply(
            res -> {
                this.log("metadata(%s): %s", key.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final long start = System.nanoTime();
        return this.origin.value(key).thenApply(
            res -> {
                this.log("value(%s): %s", key.string(), millisMessage(start));
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
                                    MeasuredStorage.this.log(
                                        "value(%s): onSubscribe(%s) %s",
                                        key.string(), subscription, millisMessage(start)
                                    );
                                    subscriber.onSubscribe(subscription);
                                }

                                @Override
                                public void onNext(final ByteBuffer buffer) {
                                    MeasuredStorage.this.log(
                                        "value(%s): onNext(%s) %s",
                                        key.string(), buffer, millisMessage(start)
                                    );
                                    subscriber.onNext(buffer);
                                }

                                @Override
                                public void onError(final Throwable err) {
                                    MeasuredStorage.this.log(
                                        "value(%s): error(%s) %s",
                                        key.string(), err, millisMessage(start)
                                    );
                                    subscriber.onError(err);
                                }

                                @Override
                                public void onComplete() {
                                    MeasuredStorage.this.log(
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
                this.log("delete(%s): %s", key.string(), millisMessage(start));
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        final long start = System.nanoTime();
        return this.origin.deleteAll(prefix).thenApply(
            res -> {
                this.log("deleteAll(%s): %s", prefix.string(), millisMessage(start));
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
     * Log message.
     *
     * @param msg The text message to be logged
     * @param args List of arguments
     */
    private void log(final String msg, final Object... args) {
        Logger.log(this.level, MeasuredStorage.class, msg, args);
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
