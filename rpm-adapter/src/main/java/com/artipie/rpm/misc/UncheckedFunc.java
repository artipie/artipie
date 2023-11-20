/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.misc;

import java.util.function.Function;

/**
 * Unchecked {@link java.util.function.Function}.
 * @param <T> Function type
 * @param <R> Function return type
 * @param <E> Error type
 * @since 0.8
 */
public final class UncheckedFunc<T, R, E extends Throwable> implements Function<T, R> {

    /**
     * Checked version.
     */
    private final Checked<T, R, E> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedFunc(final UncheckedFunc.Checked<T, R, E> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public R apply(final T val) {
        try {
            return this.checked.apply(val);
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Checked version of consumer.
     * @param <T> Consumer type
     * @param <R> Return type
     * @param <E> Error type
     * @since 0.8
     */
    @FunctionalInterface
    public interface Checked<T, R, E extends Throwable> {

        /**
         * Apply value.
         * @param value Value to accept
         * @return Result
         * @throws E On error
         */
        R apply(T value) throws E;
    }
}
