/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Matcher to verify response headers.
 */
public final class RsHasHeaders extends TypeSafeMatcher<Response> {

    /**
     * Headers matcher.
     */
    private final Matcher<? extends Iterable<? extends Header>> headers;

    /**
     * @param headers Expected headers in any order.
     */
    public RsHasHeaders(Header... headers) {
        this(Arrays.asList(headers));
    }

    /**
     * @param headers Expected header matchers in any order.
     */
    public RsHasHeaders(final Iterable<? extends Header> headers) {
        this(transform(headers));
    }

    /**
     * Ctor.
     *
     * @param headers Expected header matchers in any order.
     */
    @SafeVarargs
    public RsHasHeaders(Matcher<? super Header>... headers) {
        this(Matchers.hasItems(headers));
    }

    /**
     * Ctor.
     *
     * @param headers Headers matcher
     */
    public RsHasHeaders(
        final Matcher<? extends Iterable<? extends Header>> headers
    ) {
        this.headers = headers;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.headers);
    }

    @Override
    public boolean matchesSafely(final Response item) {
        final AtomicReference<Headers> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        return this.headers.matches(out.get());
    }

    @Override
    public void describeMismatchSafely(final Response item, final Description desc) {
        final AtomicReference<Headers> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        desc.appendText("was ").appendValue(out.get().asString());
    }

    /**
     * Transforms expected headers to expected header matchers.
     * This method is necessary to avoid compilation error.
     *
     * @param headers Expected headers in any order.
     * @return Expected header matchers in any order.
     */
    private static Matcher<? extends Iterable<Header>> transform(
        final Iterable<? extends Header> headers
    ) {
        return Matchers.allOf(
            StreamSupport.stream(headers.spliterator(), false)
                .map(Matchers::hasItem)
                .collect(Collectors.toList())
        );
    }

    /**
     * Fake connection.
     *
     * @since 0.8
     */
    static final class FakeConnection implements Connection {

        /**
         * Headers container.
         */
        private final AtomicReference<Headers> container;

        /**
         * Ctor.
         *
         * @param container Headers container
         */
        FakeConnection(final AtomicReference<Headers> container) {
            this.container = container;
        }

        @Override
        public CompletableFuture<Void> accept(
            RsStatus status,
            Headers headers,
            Publisher<ByteBuffer> body
        ) {
            return CompletableFuture.supplyAsync(
                () -> {
                    this.container.set(headers);
                    return null;
                }
            );
        }
    }
}
