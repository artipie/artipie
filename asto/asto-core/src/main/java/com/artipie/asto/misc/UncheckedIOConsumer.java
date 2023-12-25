/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.misc;

import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Unchecked IO {@link Consumer}.
 * @param <T> Consumer type
 * @since 1.1
 * @checkstyle AbbreviationAsWordInNameCheck (200 lines)
 */
public final class UncheckedIOConsumer<T> implements Consumer<T> {

    /**
     * Checked version.
     */
    private final UncheckedConsumer.Checked<T, ? extends IOException> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedIOConsumer(final UncheckedConsumer.Checked<T, ? extends IOException> checked) {
        this.checked = checked;
    }

    @Override
    public void accept(final T val) {
        try {
            this.checked.accept(val);
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
