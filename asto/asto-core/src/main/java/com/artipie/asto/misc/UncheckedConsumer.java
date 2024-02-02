/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.ArtipieException;
import java.util.function.Consumer;

/**
 * Unchecked {@link Consumer}.
 * @param <T> Consumer type
 * @param <E> Error type
 * @since 1.1
 */
public final class UncheckedConsumer<T, E extends Exception> implements Consumer<T> {

    /**
     * Checked version.
     */
    private final Checked<T, E> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedConsumer(final Checked<T, E> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void accept(final T val) {
        try {
            this.checked.accept(val);
        } catch (final Exception err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Checked version of consumer.
     * @param <T> Consumer type
     * @param <E> Error type
     * @since 1.1
     */
    @FunctionalInterface
    public interface Checked<T, E extends Exception> {

        /**
         * Accept value.
         * @param value Value to accept
         * @throws E On error
         */
        void accept(T value) throws E;
    }
}
