/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.misc;

import java.util.function.Consumer;

/**
 * Unchecked {@link Consumer}.
 * @param <T> Consumer type
 * @param <E> Error type
 * @since 0.8
 */
public final class UncheckedConsumer<T, E extends Throwable> implements Consumer<T> {

    /**
     * Checked version.
     */
    private final Checked<T, E> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedConsumer(final UncheckedConsumer.Checked<T, E> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public void accept(final T val) {
        try {
            this.checked.accept(val);
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Checked version of consumer.
     * @param <T> Consumer type
     * @param <E> Error type
     * @since 0.8
     */
    @FunctionalInterface
    public interface Checked<T, E extends Throwable> {

        /**
         * Accept value.
         * @param value Value to accept
         * @throws E On error
         */
        void accept(T value) throws E;
    }
}
