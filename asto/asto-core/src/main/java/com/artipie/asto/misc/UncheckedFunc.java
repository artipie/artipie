/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;
import java.util.function.Function;

/**
 * Unchecked {@link Function}.
 * @param <T> Function type
 * @param <R> Function return type
 * @param <E> Error type
 * @since 1.1
 */
public final class UncheckedFunc<T, R, E extends Exception> implements Function<T, R> {

    /**
     * Checked version.
     */
    private final Checked<T, R, E> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedFunc(final Checked<T, R, E> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public R apply(final T val) {
        try {
            return this.checked.apply(val);
        } catch (final Exception err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Checked version of consumer.
     * @param <T> Consumer type
     * @param <R> Return type
     * @param <E> Error type
     * @since 1.1
     */
    @FunctionalInterface
    public interface Checked<T, R, E extends Exception> {

        /**
         * Apply value.
         * @param value Value to accept
         * @return Result
         * @throws E On error
         */
        R apply(T value) throws E;
    }
}
