/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import java.io.IOException;

/**
 * Scalar that throws {@link ArtipieException} on error.
 * @param <T> Return value type
 * @since 1.3
 * @checkstyle AbbreviationAsWordInNameCheck (200 lines)
 */
public final class UncheckedIOScalar<T> implements Scalar<T> {

    /**
     * Original origin.
     */
    private final UncheckedScalar.Checked<T, ? extends IOException> origin;

    /**
     * Ctor.
     * @param origin Encapsulated origin
     */
    public UncheckedIOScalar(final UncheckedScalar.Checked<T, ? extends IOException> origin) {
        this.origin = origin;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public T value() {
        try {
            return this.origin.value();
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final IOException ex) {
            throw new ArtipieIOException(ex);
        }
    }
}
