/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Optional slice that uses some source to create new slice
 * if this source matches specified predicate.
 * @param <T> Type of target to test
 */
public final class SliceOptional<T> implements Slice {

    /**
     * Source to create a slice.
     */
    private final Supplier<? extends T> source;

    /**
     * Predicate.
     */
    private final Predicate<? super T> predicate;

    /**
     * Origin slice.
     */
    private final Function<? super T, ? extends Slice> slice;

    /**
     * New optional slice with constant source.
     * @param source Source to check
     * @param predicate Predicate checking the source
     * @param slice Slice from source
     */
    public SliceOptional(final T source,
        final Predicate<? super T> predicate,
        final Function<? super T, ? extends Slice> slice) {
        this(() -> source, predicate, slice);
    }

    /**
     * New optional slice.
     * @param source Source to check
     * @param predicate Predicate checking the source
     * @param slice Slice from source
     */
    public SliceOptional(final Supplier<? extends T> source,
        final Predicate<? super T> predicate,
        final Function<? super T, ? extends Slice> slice) {
        this.source = source;
        this.predicate = predicate;
        this.slice = slice;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers head, Content body) {
        final T target = this.source.get();
        if (this.predicate.test(target)) {
            return this.slice.apply(target).response(line, head, body);
        }
        return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
    }
}
