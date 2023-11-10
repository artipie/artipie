/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.ArtipieException;

/**
 * Exception indicates that package is invalid and so cannot be handled by repository.
 *
 * @since 0.1
 */
@SuppressWarnings("serial")
public final class InvalidPackageException extends ArtipieException {
    /**
     * Ctor.
     *
     * @param cause Underlying cause for package being invalid.
     */
    public InvalidPackageException(final Throwable cause) {
        super(cause);
    }
}
