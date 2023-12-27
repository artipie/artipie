/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

/**
 * Cleanable interface to represent objects that can be cleaned/invalidated.
 * @param <T> The key type.
 * @since 1.16
 */
public interface Cleanable<T> {

    /**
     * Invalidate object by the specified key.
     * @param key The key
     */
    void invalidate(T key);

    /**
     * Invalidate all.
     */
    void invalidateAll();

}
