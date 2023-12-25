/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;

/**
 * Scalar that throws {@link com.artipie.ArtipieException} on error.
 * @param <T> Return value type
 * @param <E> Error type
 * @since 1.3
 */
public final class UncheckedScalar<T, E extends Exception> implements Scalar<T> {

    /**
     * Original origin.
     */
    private final Checked<T, E> origin;

    /**
     * Ctor.
     * @param origin Encapsulated origin
     */
    public UncheckedScalar(final Checked<T, E> origin) {
        this.origin = origin;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public T value() {
        try {
            return this.origin.value();
        // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ex) {
            throw new ArtipieException(ex);
        }
    }

    /**
     * Checked version of scalar.
     * @param <R> Return type
     * @param <E> Error type
     * @since 1.1
     */
    @FunctionalInterface
    public interface Checked<R, E extends Exception> {

        /**
         * Return value.
         * @return Result
         * @throws E On error
         */
        R value() throws E;
    }
}
