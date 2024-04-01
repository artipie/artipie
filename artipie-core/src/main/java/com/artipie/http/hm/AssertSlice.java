/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
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
    private final Matcher<? super RequestLine> lineMatcher;

    /**
     * Request headers matcher.
     */
    private final Matcher<? super Headers> headersMatcher;

    /**
     * Request body matcher.
     */
    private final Matcher<? super Publisher<ByteBuffer>> bodyMatcher;

    /**
     * Assert slice request line.
     * @param lineMatcher Request line matcher
     */
    public AssertSlice(final Matcher<? super RequestLine> lineMatcher) {
        this(lineMatcher, Matchers.any(Headers.class), AssertSlice.STUB_BODY_MATCHER);
    }

    /**
     * @param lineMatcher Request line matcher
     * @param headersMatcher Request headers matcher
     * @param bodyMatcher Request body matcher
     */
    public AssertSlice(Matcher<? super RequestLine> lineMatcher,
                       Matcher<? super Headers> headersMatcher,
                       Matcher<? super Publisher<ByteBuffer>> bodyMatcher) {
        this.lineMatcher = lineMatcher;
        this.headersMatcher = headersMatcher;
        this.bodyMatcher = bodyMatcher;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        MatcherAssert.assertThat(
            "Wrong request line", line, this.lineMatcher
        );
        MatcherAssert.assertThat(
            "Wrong headers", headers, this.headersMatcher
        );
        MatcherAssert.assertThat(
            "Wrong body", body, this.bodyMatcher
        );
        return ResponseBuilder.ok().completedFuture();
    }
}
