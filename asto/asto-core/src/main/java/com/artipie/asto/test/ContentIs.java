/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.test;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link Content}.
 * @since 0.24
 */
public final class ContentIs extends TypeSafeMatcher<Content> {

    /**
     * Byte array matcher.
     */
    private final Matcher<byte[]> matcher;

    /**
     * Content is a string with encoding.
     * @param expected String
     * @param enc Encoding charset
     */
    public ContentIs(final String expected, final Charset enc) {
        this(expected.getBytes(enc));
    }

    /**
     * Content is a byte array.
     * @param expected Byte array
     */
    public ContentIs(final byte[] expected) {
        this(Matchers.equalTo(expected));
    }

    /**
     * Content matches for byte array matcher.
     * @param matcher Byte array matcher
     */
    public ContentIs(final Matcher<byte[]> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("has bytes ").appendValue(this.matcher);
    }

    @Override
    public boolean matchesSafely(final Content item) {
        try {
            return this.matcher.matches(
                Uninterruptibles.getUninterruptibly(
                    new PublisherAs(item).bytes().toCompletableFuture()
                )
            );
        } catch (final ExecutionException err) {
            throw new IllegalStateException("Failed to read content", err);
        }
    }
}
