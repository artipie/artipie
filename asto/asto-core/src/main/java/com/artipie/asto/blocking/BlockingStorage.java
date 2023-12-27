/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.blocking;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import java.util.Collection;

/**
 * More primitive and easy to use wrapper to use {@code Storage}.
 *
 * @since 0.1
 */
public class BlockingStorage {

    /**
     * Wrapped storage.
     */
    private final Storage storage;

    /**
     * Wrap a {@link Storage} in order get a blocking version of it.
     *
     * @param storage Storage to wrap
     */
    public BlockingStorage(final Storage storage) {
        this.storage = storage;
    }

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    public boolean exists(final Key key) {
        return this.storage.exists(key).join();
    }

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    public Collection<Key> list(final Key prefix) {
        return this.storage.list(prefix).join();
    }

    /**
     * Save the content.
     *
     * @param key The key
     * @param content The content
     */
    public void save(final Key key, final byte[] content) {
        this.storage.save(key, new Content.From(content)).join();
    }

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     */
    public void move(final Key source, final Key destination) {
        this.storage.move(source, destination).join();
    }

    /**
     * Get value size.
     *
     * @param key The key of value.
     * @return Size of value in bytes.
     * @deprecated Storage size is deprecated
     */
    @Deprecated
    public long size(final Key key) {
        return this.storage.size(key).join();
    }

    /**
     * Obtain value for the specified key.
     *
     * @param key The key
     * @return Value associated with the key
     */
    public byte[] value(final Key key) {
        return new Remaining(
            this.storage.value(key).thenApplyAsync(
                pub -> new Concatenation(pub).single().blockingGet()
            ).join(),
            true
        ).bytes();
    }

    /**
     * Removes value from storage. Fails if value does not exist.
     *
     * @param key Key for value to be deleted.
     */
    public void delete(final Key key) {
        this.storage.delete(key).join();
    }

    /**
     * Removes all items with key prefix.
     *
     * @param prefix Key prefix.
     */
    public void deleteAll(final Key prefix) {
        this.storage.deleteAll(prefix).join();
    }
}
