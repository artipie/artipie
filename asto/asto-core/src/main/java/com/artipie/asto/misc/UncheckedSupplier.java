/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;
import java.util.function.Supplier;

/**
 * Supplier to wrap checked supplier throwing checked exception
 * with unchecked one.
 * @param <T> Supplier type
 * @since 1.8
 */
public final class UncheckedSupplier<T> implements Supplier<T> {

    /**
     * Supplier which throws checked exceptions.
     */
    private final CheckedSupplier<? extends T, ? extends Exception> checked;

    /**
     * Wrap checked supplier with unchecked.
     * @param checked Checked supplier
     */
    public UncheckedSupplier(final CheckedSupplier<T, ? extends Exception> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public T get() {
        try {
            return this.checked.get();
        } catch (final Exception err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Checked supplier which throws exception.
     * @param <T> Supplier type
     * @param <E> Exception type
     * @since 1.0
     */
    @FunctionalInterface
    public interface CheckedSupplier<T, E extends Exception> {

        /**
         * Get value or throw exception.
         * @return Value
         * @throws Exception of type E
         */
        T get() throws E;
    }
}
