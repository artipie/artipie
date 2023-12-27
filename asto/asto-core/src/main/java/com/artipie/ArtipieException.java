/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

/**
 * Base Artipie exception.
 * <p>It should be used as a base exception for all Artipie public APIs
 * as a contract instead of others.</p>
 *
 * @since 1.0
 * @implNote ArtipieException is unchecked exception, but it's a good
 *  practice to document it via {@code throws} tag in JavaDocs.
 */
public class ArtipieException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * New exception with message and base cause.
     * @param msg Message
     * @param cause Cause
     */
    public ArtipieException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    /**
     * New exception with base cause.
     * @param cause Cause
     */
    public ArtipieException(final Throwable cause) {
        super(cause);
    }

    /**
     * New exception with message.
     * @param msg Message
     */
    public ArtipieException(final String msg) {
        super(msg);
    }
}
