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
}
