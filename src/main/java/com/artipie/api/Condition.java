/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

/**
 * Condition.
 * @since 0.26
 */
@FunctionalInterface
public interface Condition {
    /**
     * Validate condition.
     * @return True if valid
     */
    boolean valid();
}
