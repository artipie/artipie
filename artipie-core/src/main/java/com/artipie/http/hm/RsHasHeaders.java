/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.reactivestreams.Publisher;

/**
 * Matcher to verify response headers.
 *
 * @since 0.8
 */
public final class RsHasHeaders extends TypeSafeMatcher<Response> {

    /**
     * Headers matcher.
     */
    private final Matcher<? extends Iterable<? extends Entry<String, String>>> headers;

    /**
     * Ctor.
     *
     * @param headers Expected headers in any order.
     */
    @SafeVarargs
    public RsHasHeaders(final Entry<String, String>... headers) {
        this(Arrays.asList(headers));
    }

    /**
     * Ctor.
     *
     * @param headers Expected header matchers in any order.
     */
    public RsHasHeaders(final Iterable<? extends Entry<String, String>> headers) {
        this(transform(headers));
    }

    /**
     * Ctor.
     *
     * @param headers Expected header matchers in any order.
     */
    @SafeVarargs
    public RsHasHeaders(final Matcher<? super Entry<String, String>>... headers) {
        this(Matchers.hasItems(headers));
    }

    /**
     * Ctor.
     *
     * @param headers Headers matcher
     */
    public RsHasHeaders(
        final Matcher<? extends Iterable<? extends Entry<String, String>>> headers
    ) {
        this.headers = headers;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.headers);
    }

    @Override
    public boolean matchesSafely(final Response item) {
        final AtomicReference<Iterable<Entry<String, String>>> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        return this.headers.matches(out.get());
    }

    @Override
    public void describeMismatchSafely(final Response item, final Description desc) {
        final AtomicReference<Iterable<Entry<String, String>>> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        desc.appendText("was ").appendValue(
            StreamSupport.stream(out.get().spliterator(), false)
                .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(";"))
        );
    }

    /**
     * Transforms expected headers to expected header matchers.
     * This method is necessary to avoid compilation error.
     *
     * @param headers Expected headers in any order.
     * @return Expected header matchers in any order.
     */
    private static Matcher<? extends Iterable<Entry<String, String>>> transform(
        final Iterable<? extends Entry<String, String>> headers
    ) {
        return Matchers.allOf(
            StreamSupport.stream(headers.spliterator(), false)
                .<Entry<String, String>>map(
                    original -> new Header(
                        original.getKey(),
                        original.getValue()
                    )
                )
                .map(Matchers::hasItem)
                .collect(Collectors.toList())
        );
    }

    /**
     * Fake connection.
     *
     * @since 0.8
     */
    private static final class FakeConnection implements Connection {

        /**
         * Headers container.
         */
        private final AtomicReference<Iterable<Entry<String, String>>> container;

        /**
         * Ctor.
         *
         * @param container Headers container
         */
        FakeConnection(final AtomicReference<Iterable<Entry<String, String>>> container) {
            this.container = container;
        }

        @Override
        public CompletableFuture<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return CompletableFuture.supplyAsync(
                () -> {
                    this.container.set(
                        ImmutableList.copyOf(headers).stream().<Entry<String, String>>map(
                            original -> new Header(original.getKey(), original.getValue())
                        ).collect(Collectors.toList())
                    );
                    return null;
                }
            );
        }
    }
}
