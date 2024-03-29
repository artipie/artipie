/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.ResponseImpl;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Matcher to verify response body.
 */
public final class RsHasBody extends TypeSafeMatcher<ResponseImpl> {

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
    public boolean matchesSafely(final ResponseImpl item) {
        return this.body.matches(item.body().asBytes());
    }
}
