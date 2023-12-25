/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.util.function.Function;

/**
 * Unchecked IO {@link Function}.
 * @param <T> Function type
 * @param <R> Function return type
 * @since 1.1
 * @checkstyle AbbreviationAsWordInNameCheck (200 lines)
 */
public final class UncheckedIOFunc<T, R> implements Function<T, R> {

    /**
     * Checked version.
     */
    private final UncheckedFunc.Checked<T, R, ? extends IOException> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedIOFunc(final UncheckedFunc.Checked<T, R, ? extends IOException> checked) {
        this.checked = checked;
    }

    @Override
    public R apply(final T val) {
        try {
            return this.checked.apply(val);
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
