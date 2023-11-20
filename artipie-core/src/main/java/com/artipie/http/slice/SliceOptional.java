/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Response;
import com.artipie.http.Slice;
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
 * @since 0.21
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
