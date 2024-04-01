/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.Response;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

/**
 * Matcher to verify response body.
 */
public final class RsHasBody extends TypeSafeMatcher<Response> {

    /**
     * Body matcher.
     */
    private final Matcher<byte[]> body;

    /**
     * @param body Body to match
     */
    public RsHasBody(final byte[] body) {
        this(new IsEqual<>(body));
    }

    /**
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
        return this.body.matches(item.body().asBytes());
    }
}
