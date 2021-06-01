/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

/**
 * Single numerical value that can increase and decrease.
 *
 * @since 0.6
 */
public interface Gauge {

    /**
     * Set gauge value.
     *
     * @param value Updated value.
     */
    void set(long value);
}
