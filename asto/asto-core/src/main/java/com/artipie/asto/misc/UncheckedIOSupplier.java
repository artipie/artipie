/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Unchecked IO {@link Supplier}.
 * @param <T> Supplier type
 * @since 1.8
 * @checkstyle AbbreviationAsWordInNameCheck (200 lines)
 */
public final class UncheckedIOSupplier<T> implements Supplier<T> {

    /**
     * Checked version.
     */
    private final UncheckedSupplier.CheckedSupplier<T, ? extends IOException> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedIOSupplier(
        final UncheckedSupplier.CheckedSupplier<T, ? extends IOException> checked
    ) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public T get() {
        try {
            return this.checked.get();
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
