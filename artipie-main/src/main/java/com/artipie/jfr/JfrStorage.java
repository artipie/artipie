/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Wrapper for a Storage that generates JFR events for operations.
 *
 * @since 0.28.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle TooManyMethods (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class JfrStorage implements Storage {

    /**
     * Runnable, that does nothing.
     */
    private static final Runnable EMPTY_RUNNABLE = () -> { };

    /**
     * Original storage.
     */
    private final Storage original;

    /**
     * Ctor.
     *
     * @param original Original storage.
     */
    public JfrStorage(final Storage original) {
        this.original = original;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        final CompletableFuture<Boolean> res;
        final StorageExistsEvent event = new StorageExistsEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.exists(key)
                .thenApply(
                    exists -> this.eventProcess(exists, key, event, JfrStorage.EMPTY_RUNNABLE)
                );
        } else {
            res = this.original.exists(key);
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final CompletableFuture<Void> res;
        final StorageSaveEvent event = new StorageSaveEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.save(
                key,
                this.metricsContent(
                    key, content, event,
                    (chunks, size) -> {
                        event.chunks = chunks;
                        event.size = size;
                    }
                )
            );
        } else {
            res = this.original.save(key, content);
        }
        return res;
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> res;
        final StorageValueEvent event = new StorageValueEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.value(key)
                .thenApply(
                    content -> this.metricsContent(
                        key, content, event,
                        (chunks, size) -> {
                            event.chunks = chunks;
                            event.size = size;
                        }
                    )
                );
        } else {
            res = this.original.value(key);
        }
        return res;
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key key) {
        final CompletableFuture<Collection<Key>> res;
        final StorageListEvent event = new StorageListEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.list(key).thenApply(
                list -> this.eventProcess(
                    list, key, event, () -> event.keysCount = list.size()
                )
            );
        } else {
            res = this.original.list(key);
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key target) {
        final CompletableFuture<Void> res;
        final StorageMoveEvent event = new StorageMoveEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.move(source, target)
                .thenRun(
                    () -> this.eventProcess(source, event, () -> event.target = target.string())
                );
        } else {
            res = this.original.move(source, target);
        }
        return res;
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        final CompletableFuture<? extends Meta> res;
        final StorageMetadataEvent event = new StorageMetadataEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.metadata(key)
                .thenApply(
                    metadata -> this.eventProcess(metadata, key, event, JfrStorage.EMPTY_RUNNABLE)
                );
        } else {
            res = this.original.metadata(key);
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        final CompletableFuture<Void> res;
        final StorageDeleteEvent event = new StorageDeleteEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.delete(key)
                .thenRun(
                    () -> this.eventProcess(key, event, JfrStorage.EMPTY_RUNNABLE)
                );
        } else {
            res = this.original.delete(key);
        }
        return res;
    }

    @Override
    public <T> CompletionStage<T> exclusively(final Key key,
        final Function<Storage, CompletionStage<T>> function) {
        final CompletionStage<T> res;
        final StorageExclusivelyEvent event = new StorageExclusivelyEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.exclusively(key, function)
                .thenApply(
                    fres -> this.eventProcess(fres, key, event, JfrStorage.EMPTY_RUNNABLE)
                );
        } else {
            res = this.original.exclusively(key, function);
        }
        return res;
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        final CompletableFuture<Void> res;
        final StorageDeleteAllEvent event = new StorageDeleteAllEvent();
        if (event.isEnabled()) {
            event.begin();
            res = this.original.deleteAll(prefix)
                .thenRun(
                    () -> this.eventProcess(prefix, event, JfrStorage.EMPTY_RUNNABLE)
                );
        } else {
            res = this.original.deleteAll(prefix);
        }
        return res;
    }

    @Override
    public String identifier() {
        return this.original.identifier();
    }

    /**
     * Wraps passed {@code content} to {@link ChunksAndSizeMetricsContent}.
     *
     * @param key Key
     * @param content Content
     * @param evt JFR event
     * @param updater Lambda to fulfill an event`s fields
     * @return Wrapped content
     * @checkstyle ParameterNumberCheck (25 lines)
     */
    private ChunksAndSizeMetricsContent metricsContent(
        final Key key,
        final Content content,
        final AbstractStorageEvent evt,
        final BiConsumer<Integer, Long> updater
    ) {
        return new ChunksAndSizeMetricsContent(
            content,
            (chunks, size) -> this.eventProcess(
                key, evt, () -> updater.accept(chunks, size)
            )
        );
    }

    /**
     * If {@code event} should be commit then fulfills an event`s fields and commits.
     *
     * @param key Key
     * @param evt JFR event
     * @param updater Lambda to fulfill an event`s fields
     */
    private void eventProcess(
        final Key key,
        final AbstractStorageEvent evt,
        final Runnable updater
    ) {
        this.eventProcess(null, key, evt, updater);
    }

    /**
     * If {@code event} should be commit then fulfills an event`s fields and commits.
     *
     * @param res Result
     * @param key Key
     * @param evt JFR event
     * @param updater Lambda to fulfill an event`s fields
     * @param <T> Result type
     * @return Result
     * @checkstyle ParameterNumberCheck (25 lines)
     */
    private <T> T eventProcess(
        final T res,
        final Key key,
        final AbstractStorageEvent evt,
        final Runnable updater
    ) {
        evt.end();
        if (evt.shouldCommit()) {
            evt.storage = this.identifier();
            evt.key = key.string();
            updater.run();
            evt.commit();
        }
        return res;
    }
}
