/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.reactivestreams.Publisher;

/**
 * Matcher to verify response body.
 *
 * @since 0.1
 */
public final class RsHasBody extends TypeSafeMatcher<Response> {

    /**
     * Body matcher.
     */
    private final Matcher<byte[]> body;

    /**
     * Check response has string body in charset.
     * @param body Body string
     */
    public RsHasBody(final String body) {
        this(Matchers.is(body), StandardCharsets.UTF_8);
    }

    /**
     * Check response has string body in charset.
     * @param body Body string
     * @param charset Charset encoding
     */
    public RsHasBody(final String body, final Charset charset) {
        this(Matchers.is(body), charset);
    }

    /**
     * Check response has string body in charset.
     * @param body Body string
     * @param charset Charset encoding
     */
    public RsHasBody(final Matcher<String> body, final Charset charset) {
        this(new IsString(charset, body));
    }

    /**
     * Ctor.
     *
     * @param body Body to match
     */
    public RsHasBody(final byte[] body) {
        this(new IsEqual<>(body));
    }

    /**
     * Ctor.
     *
     * @param body Body matcher
     */
    public RsHasBody(final Matcher<byte[]> body) {
        this.body = body;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.body);
    }

    @Override
    public boolean matchesSafely(final Response item) {
        final AtomicReference<byte[]> out = new AtomicReference<>();
        item.send(new FakeConnection(out)).toCompletableFuture().join();
        return this.body.matches(out.get());
    }

    /**
     * Fake connection.
     *
     * @since 0.1
     */
    private static final class FakeConnection implements Connection {

        /**
         * Body container.
         */
        private final AtomicReference<byte[]> container;

        /**
         * Ctor.
         *
         * @param container Body container
         */
        FakeConnection(final AtomicReference<byte[]> container) {
            this.container = container;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> body
        ) {
            return CompletableFuture.supplyAsync(
                () -> {
                    final ByteBuffer buffer = Flowable.fromPublisher(body)
                        .toList()
                        .blockingGet()
                        .stream()
                        .reduce(
                            (left, right) -> {
                                left.mark();
                                right.mark();
                                final ByteBuffer concat = ByteBuffer.allocate(
                                    left.remaining() + right.remaining()
                                ).put(left).put(right);
                                left.reset();
                                right.reset();
                                concat.flip();
                                return concat;
                            }
                        )
                        .orElse(ByteBuffer.allocate(0));
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.mark();
                    buffer.get(bytes);
                    buffer.reset();
                    this.container.set(bytes);
                    return null;
                }
            );
        }
    }

}
