/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;

/**
 * Optional slice that uses some source to create new slice
 * if this source matches specified predicate.
 * @param <T> Type of target to test
 * @since 0.10
 * @todo #425:30min Create a test for this slice.
 *  The test should verify that slice returns response from origin slice if
 *  condition matches, and returns 404 if it doesn't match.
 */
public final class OptionalSlice<T> implements Slice {

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
    public OptionalSlice(final T source,
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
    public OptionalSlice(final Supplier<? extends T> source,
        final Predicate<? super T> predicate,
        final Function<? super T, ? extends Slice> slice) {
        this.source = source;
        this.predicate = predicate;
        this.slice = slice;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> head,
        final Publisher<ByteBuffer> body) {
        final Response response;
        final T target = this.source.get();
        if (this.predicate.test(target)) {
            response = this.slice.apply(target).response(line, head, body);
        } else {
            response = new RsWithStatus(RsStatus.NOT_FOUND);
        }
        return response;
    }
}
