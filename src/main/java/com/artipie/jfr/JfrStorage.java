/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Wrapper for a Storage that generates JFR events for operations.
 *
 * @since 0.28.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class JfrStorage implements Storage {

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
        final StorageExistsEvent event = new StorageExistsEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.exists(key)
            .thenApply(
                res -> {
                    event.commit();
                    return res;
                }
            );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final StorageSaveEvent event = new StorageSaveEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.save(
            key,
            new ChunksAndSizeMetricsContent(
                content,
                (chunks, size) -> {
                    event.chunks = chunks;
                    event.size = size;
                    event.commit();
                }
            )
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key key) {
        final StorageListEvent event = new StorageListEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.list(key).thenApply(
            res -> {
                event.keysCount = res.size();
                event.commit();
                return res;
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key target) {
        final StorageMoveEvent event = new StorageMoveEvent();
        event.storage = this.identifier();
        event.key = source.string();
        event.target = target.string();
        event.begin();
        return this.original.move(source, target)
            .thenRun(event::commit);
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        final StorageMetadataEvent event = new StorageMetadataEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.metadata(key)
            .thenApply(
                res -> {
                    event.commit();
                    return res;
                }
            );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final StorageValueEvent event = new StorageValueEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.value(key)
            .thenApply(
                content -> new ChunksAndSizeMetricsContent(
                    content,
                    (chunks, size) -> {
                        event.chunks = chunks;
                        event.size = size;
                        event.commit();
                    }
                )
            );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        final StorageDeleteEvent event = new StorageDeleteEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.delete(key)
            .thenRun(event::commit);
    }

    @Override
    public <T> CompletionStage<T> exclusively(final Key key,
        final Function<Storage, CompletionStage<T>> function) {
        final StorageExclusivelyEvent event = new StorageExclusivelyEvent();
        event.storage = this.identifier();
        event.key = key.string();
        event.begin();
        return this.original.exclusively(key, function)
            .thenApply(
                res -> {
                    event.commit();
                    return res;
                }
            );
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        final StorageDeleteAllEvent event = new StorageDeleteAllEvent();
        event.storage = this.identifier();
        event.key = prefix.string();
        event.begin();
        return this.original.deleteAll(prefix)
            .thenRun(event::commit);
    }

    @Override
    public String identifier() {
        return this.original.identifier();
    }
}
