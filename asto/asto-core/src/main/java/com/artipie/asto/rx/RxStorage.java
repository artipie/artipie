/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.rx;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Collection;
import java.util.function.Function;

/**
 * A reactive version of {@link com.artipie.asto.Storage}.
 *
 * @since 0.10
 */
public interface RxStorage {

    /**
     * This file exists?
     *
     * @param key The key (file name)
     * @return TRUE if exists, FALSE otherwise
     */
    Single<Boolean> exists(Key key);

    /**
     * Return the list of keys that start with this prefix, for
     * example "foo/bar/".
     *
     * @param prefix The prefix.
     * @return Collection of relative keys.
     */
    Single<Collection<Key>> list(Key prefix);

    /**
     * Saves the bytes to the specified key.
     *
     * @param key The key
     * @param content Bytes to save
     * @return Completion or error signal.
     */
    Completable save(Key key, Content content);

    /**
     * Moves value from one location to another.
     *
     * @param source Source key.
     * @param destination Destination key.
     * @return Completion or error signal.
     */
    Completable move(Key source, Key destination);

    /**
     * Get value size.
     *
     * @param key The key of value.
     * @return Size of value in bytes.
     */
    Single<Long> size(Key key);

    /**
     * Obtain bytes by key.
     *
     * @param key The key
     * @return Bytes.
     */
    Single<Content> value(Key key);

    /**
     * Removes value from storage. Fails if value does not exist.
     *
     * @param key Key for value to be deleted.
     * @return Completion or error signal.
     */
    Completable delete(Key key);

    /**
     * Runs operation exclusively for specified key.
     *
     * @param key Key which is scope of operation.
     * @param operation Operation to be performed exclusively.
     * @param <T> Operation result type.
     * @return Result of operation.
     */
    <T> Single<T> exclusively(
        Key key,
        Function<RxStorage, Single<T>> operation
    );
}
