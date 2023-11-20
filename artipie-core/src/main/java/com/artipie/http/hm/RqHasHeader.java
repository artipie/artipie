/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;
import java.util.Collections;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher verifies that request headers filtered by name are matched against target matcher.
 * @since 0.10
 */
public class RqHasHeader extends TypeSafeMatcher<Headers> {

    /**
     * Headers name.
     */
    private final String name;

    /**
     * Headers matcher.
     */
    private final Matcher<? extends Iterable<? extends String>> matcher;

    /**
     * Match headers by name against target matcher.
     * @param name Headers name
     * @param matcher Target matcher
     */
    public RqHasHeader(final String name,
        final Matcher<? extends Iterable<? extends String>> matcher) {
        this.name = name;
        this.matcher = matcher;
    }

    @Override
    public final boolean matchesSafely(final Headers item) {
        return this.matcher.matches(new RqHeaders(item, this.name));
    }

    @Override
    public final void describeTo(final Description description) {
        description.appendText(String.format("Headers '%s': ", this.name))
            .appendDescriptionOf(this.matcher);
    }

    /**
     * Matcher for single header.
     * @since 0.10
     */
    public static final class Single extends RqHasHeader {

        /**
         * Matche header by name against value matcher.
         * @param name Header name
         * @param header Value matcher
         */
        public Single(final String name, final Matcher<String> header) {
            super(name, Matchers.contains(Collections.singletonList(header)));
        }

        /**
         * Match header by name against its value.
         * @param name Header name
         * @param header Header value
         */
        public Single(final String name, final String header) {
            this(name, Matchers.equalTo(header));
        }
    }
}
