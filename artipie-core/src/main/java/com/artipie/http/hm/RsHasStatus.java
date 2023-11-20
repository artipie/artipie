/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.reactivestreams.Publisher;

/**
 * Matcher to verify response status.
 * @since 0.1
 */
public final class RsHasStatus extends TypeSafeMatcher<Response> {

    /**
     * Status code matcher.
     */
    private final Matcher<RsStatus> status;

    /**
     * Ctor.
     * @param status Code to match
     */
    public RsHasStatus(final RsStatus status) {
        this(new IsEqual<>(status));
    }

    /**
     * Ctor.
     * @param status Code matcher
     */
    public RsHasStatus(final Matcher<RsStatus> status) {
        this.status = status;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.status);
    }

    @Override
    public boolean matchesSafely(final Response item) {
        final AtomicReference<RsStatus> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        return this.status.matches(out.get());
    }

    /**
     * Fake connection.
     * @since 0.1
     */
    private static final class FakeConnection implements Connection {

        /**
         * Status code container.
         */
        private final AtomicReference<RsStatus> container;

        /**
         * Ctor.
         * @param container Status code container
         */
        FakeConnection(final AtomicReference<RsStatus> container) {
            this.container = container;
        }

        @Override
        public CompletableFuture<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return CompletableFuture.supplyAsync(
                () -> {
                    this.container.set(status);
                    return null;
                }
            );
        }
    }
}
