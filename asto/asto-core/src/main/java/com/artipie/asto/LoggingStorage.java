/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage that logs performed operations.
 *
 * @since 0.20.4
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.LoggerIsNotStaticFinal"})
public final class LoggingStorage implements Storage {

    /**
     * Logger.
     */
    private final Logger logger;

    /**
     * Logging level.
     */
    private final Level level;

    /**
     * Delegate storage.
     */
    private final Storage storage;

    /**
     * Storage string identifier.
     */
    private final String id;

    /**
     * Ctor.
     *
     * @param storage Delegate storage.
     */
    public LoggingStorage(final Storage storage) {
        this(Level.FINE, storage);
    }

    /**
     * Ctor.
     *
     * @param level Logging level.
     * @param storage Delegate storage.
     */
    public LoggingStorage(final Level level, final Storage storage) {
        this.level = level;
        this.storage = storage;
        this.logger = Logger.getLogger(
            this.storage.getClass().getCanonicalName()
        );
        this.id = String.format("Logging: %s", this.storage.identifier());
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.storage.exists(key).thenApply(
            result -> {
                this.log("Exists '%s': %s", key.string(), result);
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return this.storage.list(prefix).thenApply(
            result -> {
                this.log("List '%s': %s", prefix.string(), result.size());
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.storage.save(key, content).thenApply(
            result -> {
                this.log(
                        "Save '%s': %s",
                        key.string(),
                        content.size().map(String::valueOf).orElse("unknown size"));
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.storage.move(source, destination).thenApply(
            result -> {
                this.log("Move '%s' '%s'", source.string(), destination.string());
                return result;
            }
        );
    }

    // @checkstyle MissingDeprecatedCheck (5 lines)
    @Override
    @Deprecated
    public CompletableFuture<Long> size(final Key key) {
        return this.storage.size(key).thenApply(
            result -> {
                this.log("Size '%s': %s", key.string(), result);
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.storage.value(key).thenApply(
            result -> {
                this.log("Value '%s': %s", key.string(), result.size());
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.storage.delete(key).thenApply(
            result -> {
                this.log("Delete '%s'", key.string());
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<Void> deleteAll(final Key prefix) {
        return this.storage.deleteAll(prefix).thenApply(
            result -> {
                this.log("Delete all keys prefixed by '%s'", prefix.string());
                return result;
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return this.storage.exclusively(key, operation).thenApply(
            result -> {
                this.log("Exclusively for '%s': %s", key, operation);
                return result;
            }
        );
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        return this.storage.metadata(key).thenApply(
            result -> {
                this.log("Metadata '%s': %s", key.string(), result);
                return result;
            }
        );
    }

    @Override
    public String identifier() {
        return this.id;
    }

    /**
     * Log message.
     *
     * @param msg The text message to be logged.
     * @param args Optional arguments for string formatting.
     */
    private void log(final String msg, final Object... args) {
        this.logger.log(this.level, String.format(msg, args));
    }
}
