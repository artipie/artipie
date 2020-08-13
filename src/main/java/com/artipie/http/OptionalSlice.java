/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
