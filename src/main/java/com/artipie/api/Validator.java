/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

/**
 * Validator.
 * @since 0.26
 */
public interface Validator {
    /**
     * Validate.
     *
     * @return True if valid
     */
    boolean isValid();

    /**
     * Provides error message if not valid.
     * @return Error message
     */
    String errorMessage();

    /**
     * Validates and runs callback if not valid.
     * @param callback Callback
     * @return Result of validation
     */
    default boolean validate(Runnable callback) {
        final boolean result = this.isValid();
        if (result) {
            callback.run();
        }
        return result;
    }
}
