/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.FailedCompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Translate an exception happened inside future.
 *
 * @param <T> Future result type.
 * @since 0.1
 */
final class InternalExceptionHandle<T> implements BiFunction<T, Throwable, CompletionStage<T>> {

    /**
     * Type of exception to handle.
     */
    private final Class<? extends Throwable> from;

    /**
     * Converter to a new exception.
     */
    private final Function<? super Throwable, ? extends Throwable> convert;

    /**
     * Ctor.
     *
     * @param from Internal type of exception.
     * @param convert Converter to a external type.
     */
    InternalExceptionHandle(
        final Class<? extends Throwable> from,
        final Function<? super Throwable, ? extends Throwable> convert
    ) {
        this.from = from;
        this.convert = convert;
    }

    @Override
    public CompletionStage<T> apply(final T content, final Throwable throwable) {
        final CompletionStage<T> result;
        if (throwable == null) {
            result = CompletableFuture.completedFuture(content);
        } else {
            if (
                throwable instanceof CompletionException
                    && this.from.isInstance(throwable.getCause())
            ) {
                result = new FailedCompletionStage<>(
                    this.convert.apply(throwable.getCause())
                );
            } else if (throwable instanceof CompletionException) {
                result = new FailedCompletionStage<>(new ArtipieIOException(throwable.getCause()));
            } else {
                result = new FailedCompletionStage<>(new ArtipieIOException(throwable));
            }
        }
        return result;
    }
}
