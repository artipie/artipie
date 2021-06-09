/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

/**
 * Monotonically increasing cumulative counter.
 *
 * @since 0.6
 */
public interface Counter {

    /**
     * Add amount to counter value.
     *
     * @param amount Amount to be added to counter.
     */
    void add(long amount);

    /**
     * Increment counter value. Shortcut for <code>add(1)</code>.
     */
    void inc();
}
