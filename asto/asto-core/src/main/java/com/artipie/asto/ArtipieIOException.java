/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Artipie input-output exception.
 * @since 1.0
 */
public class ArtipieIOException extends ArtipieException {

    private static final long serialVersionUID = 862160427262047490L;

    /**
     * New IO excption.
     * @param cause IO exception
     */
    public ArtipieIOException(final IOException cause) {
        super(cause);
    }

    /**
     * New IO excption with message.
     * @param msg Message
     * @param cause IO exception
     */
    public ArtipieIOException(final String msg, final IOException cause) {
        super(msg, cause);
    }

    /**
     * New IO exception.
     * @param cause Unkown exception
     */
    public ArtipieIOException(final Throwable cause) {
        this(ArtipieIOException.unwrap(cause));
    }

    /**
     * New IO exception.
     * @param msg Exception message
     * @param cause Unkown exception
     */
    public ArtipieIOException(final String msg, final Throwable cause) {
        this(msg, ArtipieIOException.unwrap(cause));
    }

    /**
     * New IO exception with message.
     * @param msg Exception message
     */
    public ArtipieIOException(final String msg) {
        this(new IOException(msg));
    }

    /**
     * Resolve unkown exception to IO exception.
     * @param cause Unkown exception
     * @return IO exception
     */
    private static IOException unwrap(final Throwable cause) {
        final IOException iex;
        if (cause instanceof UncheckedIOException) {
            iex = ((UncheckedIOException) cause).getCause();
        } else if (cause instanceof IOException) {
            iex = (IOException) cause;
        } else {
            iex = new IOException(cause);
        }
        return iex;
    }
}
