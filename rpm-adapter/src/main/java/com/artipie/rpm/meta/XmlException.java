/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

/**
 * Various error/problems with xml parsing/reading/writing.
 * @since 1.3
 */
@SuppressWarnings("serial")
public final class XmlException extends RuntimeException {

    /**
     * Ctor.
     * @param cause Error cause
     */
    public XmlException(final Throwable cause) {
        super(cause);
    }

    /**
     * Ctor.
     * @param message Error message
     */
    XmlException(final String message) {
        super(message);
    }

    /**
     * Ctor.
     * @param message Message
     * @param cause Error cause
     */
    public XmlException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
