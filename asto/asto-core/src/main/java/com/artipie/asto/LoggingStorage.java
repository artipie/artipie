/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Storage that logs performed operations with INFO level.
 */
public class LoggingStorage implements Storage {

    private static final Logger LOGGER = LoggerFactory.getLogger(Storage.class);

    /**
     * Delegate storage.
     */
    private final Storage original;

    /**
     * Storage string identifier.
     */
    private final String id;

    /**
     * Ctor.
     *
     * @param original Delegate storage.
     */
    public LoggingStorage(final Storage original) {
        this.original = original;
        this.id = "Slf4jLogger: " + this.original.identifier();
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.exists(key).thenApply(
                    result -> {
                        LOGGER.info("Exists '{}': {}", key.string(), result);
                        return result;
                    }
            );
        }
        return this.original.exists(key);
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.list(prefix).thenApply(
                    result -> {
                        LOGGER.info("List '{}': {}", prefix.string(), result.size());
                        return result;
                    }
            );
        }
        return this.original.list(prefix);
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.save(key, content).thenApply(
                    result -> {
                        LOGGER.info("Save '{}': {}", key.string(), content.size());
                        return result;
                    }
            );
        }
        return this.original.save(key, content);
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.move(source, destination).thenApply(
                    result -> {
                        LOGGER.info("Move '{}' '{}'", source.string(), destination.string());
                        return result;
                    }
            );
        }
        return this.original.move(source, destination);
    }

    @Override
    @Deprecated
    public CompletableFuture<Long> size(final Key key) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.size(key).thenApply(
                    result -> {
                        LOGGER.info("Size '{}': {}", key.string(), result);
                        return result;
                    }
            );
        }
        return this.original.size(key);
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.value(key).thenApply(
                    result -> {
                        LOGGER.info("Value '{}': {}", key.string(), result.size());
                        return result;
                    }
            );
        }
        return this.original.value(key);
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.delete(key).thenApply(
                    result -> {
                        LOGGER.info("Delete '{}'", key.string());
                        return result;
                    }
            );
        }
        return this.original.delete(key);
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.deleteAll(prefix).thenApply(
                    result -> {
                        LOGGER.info("Delete all keys prefixed by '{}'", prefix.string());
                        return result;
                    }
            );
        }
        return this.original.deleteAll(prefix);
    }

    @Override
    public <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> operation
    ) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.exclusively(key, operation).thenApply(
                    result -> {
                        LOGGER.info("Exclusively for '{}': {}", key, operation);
                        return result;
                    }
            );
        }
        return this.original.exclusively(key, operation);
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        if (LOGGER.isInfoEnabled()) {
            return this.original.metadata(key).thenApply(
                    result -> {
                        LOGGER.info("Metadata '{}': {}", key, result);
                        return result;
                    }
            );
        }
        return this.original.metadata(key);
    }

    @Override
    public String identifier() {
        return this.id;
    }
}
