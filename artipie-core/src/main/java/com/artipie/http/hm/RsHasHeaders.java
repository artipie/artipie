/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.ResponseImpl;
import com.artipie.http.headers.Header;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Matcher to verify response headers.
 */
public final class RsHasHeaders extends TypeSafeMatcher<ResponseImpl> {

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
     * @param headers Expected header matchers in any order.
     */
    @SafeVarargs
    public RsHasHeaders(Matcher<? super Header>... headers) {
        this(Matchers.hasItems(headers));
    }

    /**
     * @param headers Headers matcher
     */
    public RsHasHeaders(Matcher<? extends Iterable<? extends Header>> headers) {
        this.headers = headers;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.headers);
    }

    @Override
    public boolean matchesSafely(final ResponseImpl item) {
        return this.headers.matches(item.headers());
    }

    @Override
    public void describeMismatchSafely(final ResponseImpl item, final Description desc) {
        desc.appendText("was ").appendValue(item.headers().asString());
    }

    /**
     * Transforms expected headers to expected header matchers.
     * This method is necessary to avoid compilation error.
     *
     * @param headers Expected headers in any order.
     * @return Expected header matchers in any order.
     */
    private static Matcher<? extends Iterable<Header>> transform(Iterable<? extends Header> headers) {
        return Matchers.allOf(
            StreamSupport.stream(headers.spliterator(), false)
                .map(Matchers::hasItem)
                .collect(Collectors.toList())
        );
    }
}
