/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;
import com.artipie.asto.fs.FileStorage;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The storage.
 * <p>
 * You are supposed to implement this interface the way you want. It has
 * to abstract the binary storage. You may use {@link FileStorage} if you
 * want to work with files. Otherwise, for S3 or something else, you have
 * to implement it yourself.
 */
public interface Storage {

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    CompletableFuture<Boolean> exists(Key key);

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    CompletableFuture<Collection<Key>> list(Key prefix);

    /**
     * Saves the bytes to the specified key.
     *
     * @param key The key
     * @param content Bytes to save
     * @return Completion or error signal.
     */
    CompletableFuture<Void> save(Key key, Content content);

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     * @return Completion or error signal.
     */
    CompletableFuture<Void> move(Key source, Key destination);

    /**
     * Get value size.
     *
     * @param key The key of value.
     * @return Size of value in bytes.
     * @deprecated Use {@link #metadata(Key)} to get content size
     */
    @Deprecated
    default CompletableFuture<Long> size(final Key key) {
        return this.metadata(key).thenApply(
            meta -> meta.read(Meta.OP_SIZE).orElseThrow(
                () -> new ArtipieException(
                    String.format("SIZE could't be read for %s key", key.string())
                )
            )
        );
    }

    /**
     * Get content metadata.
     * @param key Content key
     * @return Future with metadata
     */
    CompletableFuture<? extends Meta> metadata(Key key);

    /**
     * Obtain bytes by key.
     *
     * @param key The key
     * @return Bytes.
     */
    CompletableFuture<Content> value(Key key);

    /**
     * Removes value from storage. Fails if value does not exist.
     *
     * @param key Key for value to be deleted.
     * @return Completion or error signal.
     */
    CompletableFuture<Void> delete(Key key);

    /**
     * Removes all items with key prefix.
     *
     * @implNote It is important that keys are deleted sequentially.
     * @param prefix Key prefix.
     * @return Completion or error signal.
     */
    default CompletableFuture<Void> deleteAll(final Key prefix) {
        return this.list(prefix).thenCompose(
            keys -> {
                CompletableFuture<Void> res = CompletableFuture.allOf();
                for (final Key key : keys) {
                    res = res.thenCompose(noth -> this.delete(key));
                }
                return res;
            }
        );
    }

    /**
     * Runs operation exclusively for specified key.
     *
     * @param key Key which is scope of operation.
     * @param operation Operation to be performed exclusively.
     * @param <T> Operation result type.
     * @return Result of operation.
     */
    <T> CompletionStage<T> exclusively(
        Key key,
        Function<Storage, CompletionStage<T>> operation
    );

    /**
     * Get storage identifier. Returned string should allow identifying storage and provide some
     * unique storage information allowing to concrete determine storage instance. For example, for
     * file system storage, it could provide the type and path, for s3 - base url and username.
     * @return String storage identifier
     */
    default String identifier() {
        return getClass().getSimpleName();
    }

    /**
     * Forwarding decorator for {@link Storage}.
     *
     * @since 0.18
     */
    abstract class Wrap implements Storage {

        /**
         * Delegate storage.
         */
        private final Storage delegate;

        /**
         * @param delegate Delegate storage
         */
        protected Wrap(final Storage delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletableFuture<Boolean> exists(final Key key) {
            return this.delegate.exists(key);
        }

        @Override
        public CompletableFuture<Collection<Key>> list(final Key prefix) {
            return this.delegate.list(prefix);
        }

        @Override
        public CompletableFuture<Void> save(final Key key, final Content content) {
            return this.delegate.save(key, content);
        }

        @Override
        public CompletableFuture<Void> move(final Key source, final Key destination) {
            return this.delegate.move(source, destination);
        }

        @Override
        public CompletableFuture<Long> size(final Key key) {
            return this.delegate.size(key);
        }

        @Override
        public CompletableFuture<Content> value(final Key key) {
            return this.delegate.value(key);
        }

        @Override
        public CompletableFuture<Void> delete(final Key key) {
            return this.delegate.delete(key);
        }

        @Override
        public CompletableFuture<Void> deleteAll(final Key prefix) {
            return this.delegate.deleteAll(prefix);
        }

        @Override
        public <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> operation
        ) {
            return this.delegate.exclusively(key, operation);
        }

        @Override
        public CompletableFuture<? extends Meta> metadata(final Key key) {
            return this.delegate.metadata(key);
        }

        @Override
        public String identifier() {
            return this.delegate.identifier();
        }
    }
}
