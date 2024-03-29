/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Slice implementation which assert request data against specified matchers.
 */
public final class AssertSlice implements Slice {

    /**
     * Always true type safe matcher for publisher.
     * @since 0.10
     */
    private static final TypeSafeMatcher<Publisher<ByteBuffer>> STUB_BODY_MATCHER =
        new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(final Publisher<ByteBuffer> item) {
                return true;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("stub");
            }
        };

    /**
     * Request line matcher.
     */
    private final Matcher<? super RequestLine> line;

    /**
     * Request headers matcher.
     */
    private final Matcher<? super Headers> head;

    /**
     * Request body matcher.
     */
    private final Matcher<? super Publisher<ByteBuffer>> body;

    /**
     * Assert slice request line.
     * @param line Request line matcher
     */
    public AssertSlice(final Matcher<? super RequestLine> line) {
        this(line, Matchers.any(Headers.class), AssertSlice.STUB_BODY_MATCHER);
    }

    /**
     * Ctor.
     * @param line Request line matcher
     * @param head Request headers matcher
     * @param body Request body matcher
     */
    public AssertSlice(final Matcher<? super RequestLine> line,
        final Matcher<? super Headers> head, final Matcher<? super Publisher<ByteBuffer>> body) {
        this.line = line;
        this.head = head;
        this.body = body;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine lne, Headers headers, Content publ) {
        MatcherAssert.assertThat(
            "Wrong request line", lne, this.line
        );
        MatcherAssert.assertThat(
            "Wrong headers", headers, this.head
        );
        MatcherAssert.assertThat(
            "Wrong body", publ, this.body
        );
        return ResponseBuilder.ok().completedFuture();
    }
}
