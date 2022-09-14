/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

/**
 * Error message.
 * @since 0.26
 */
@FunctionalInterface
public interface ErrorMessage {
    /**
     * Getter for error message.
     * @return Error message
     */
    String message();
}
