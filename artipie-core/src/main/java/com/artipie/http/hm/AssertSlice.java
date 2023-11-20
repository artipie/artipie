/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.reactivestreams.Publisher;

/**
 * Slice implementation which assert request data against specified matchers.
 * @since 0.10
 */
public final class AssertSlice implements Slice {

    /**
     * Always true type safe matcher for publisher.
     * @since 0.10
     */
    private static final TypeSafeMatcher<Publisher<ByteBuffer>> STUB_BODY_MATCHER =
        new TypeSafeMatcher<Publisher<ByteBuffer>>() {
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
    private final Matcher<? super RequestLineFrom> line;

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
    public AssertSlice(final Matcher<? super RequestLineFrom> line) {
        this(line, Matchers.any(Headers.class), AssertSlice.STUB_BODY_MATCHER);
    }

    /**
     * Ctor.
     * @param line Request line matcher
     * @param head Request headers matcher
     * @param body Request body matcher
     */
    public AssertSlice(final Matcher<? super RequestLineFrom> line,
        final Matcher<? super Headers> head, final Matcher<? super Publisher<ByteBuffer>> body) {
        this.line = line;
        this.head = head;
        this.body = body;
    }

    @Override
    public Response response(final String lne, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publ) {
        MatcherAssert.assertThat(
            "Wrong request line", new RequestLineFrom(lne), this.line
        );
        MatcherAssert.assertThat(
            "Wrong headers", new Headers.From(headers), this.head
        );
        MatcherAssert.assertThat(
            "Wrong body", publ, this.body
        );
        return StandardRs.EMPTY;
    }
}
