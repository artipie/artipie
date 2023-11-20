/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.ArtipieException;

/**
 * Exception indicates that package version cannot be added,
 * because it is already exists in the storage.
 *
 * @since 0.1
 */
@SuppressWarnings("serial")
public final class PackageVersionAlreadyExistsException extends ArtipieException {

    /**
     * Ctor.
     *
     * @param message Exception details message.
     */
    public PackageVersionAlreadyExistsException(final String message) {
        super(message);
    }
}
