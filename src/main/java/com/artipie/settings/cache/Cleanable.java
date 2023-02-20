/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

/**
 * Cleanable object provide an opportunity to invalidate its cache or some
 * other items.
 * @since 0.27
 */
public interface Cleanable {

    /**
     * Perform invalidation.
     */
    void invalidate();
}
