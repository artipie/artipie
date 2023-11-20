/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import java.nio.charset.Charset;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

/**
 * Matcher to verify byte array as string.
 *
 * @since 0.7.2
 */
public final class IsString extends TypeSafeMatcher<byte[]> {

    /**
     * Charset used to decode bytes to string.
     */
    private final Charset charset;

    /**
     * String matcher.
     */
    private final Matcher<String> matcher;

    /**
     * Ctor.
     *
     * @param string String the bytes should be equal to.
     */
    public IsString(final String string) {
        this(Charset.defaultCharset(), new IsEqual<>(string));
    }

    /**
     * Ctor.
     *
     * @param charset Charset used to decode bytes to string.
     * @param string String the bytes should be equal to.
     */
    public IsString(final Charset charset, final String string) {
        this(charset, new IsEqual<>(string));
    }

    /**
     * Ctor.
     *
     * @param matcher Matcher for string.
     */
    public IsString(final Matcher<String> matcher) {
        this(Charset.defaultCharset(), matcher);
    }

    /**
     * Ctor.
     *
     * @param charset Charset used to decode bytes to string.
     * @param matcher Matcher for string.
     */
    public IsString(final Charset charset, final Matcher<String> matcher) {
        this.charset = charset;
        this.matcher = matcher;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("bytes ").appendDescriptionOf(this.matcher);
    }

    @Override
    public boolean matchesSafely(final byte[] item) {
        final String string = new String(item, this.charset);
        return this.matcher.matches(string);
    }
}
