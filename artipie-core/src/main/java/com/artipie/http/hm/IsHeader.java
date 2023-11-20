/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.IsEqualIgnoringCase;

/**
 * Header matcher.
 *
 * @since 0.8
 */
public final class IsHeader extends TypeSafeMatcher<Map.Entry<String, String>> {

    /**
     * Name matcher.
     */
    private final Matcher<String> name;

    /**
     * Value matcher.
     */
    private final Matcher<String> value;

    /**
     * Ctor.
     *
     * @param name Expected header name, compared ignoring case.
     * @param value Expected header value.
     */
    public IsHeader(final String name, final String value) {
        this(name, new IsEqual<>(value));
    }

    /**
     * Ctor.
     *
     * @param name Expected header name, compared ignoring case.
     * @param value Value matcher.
     */
    public IsHeader(final String name, final Matcher<String> value) {
        this(new IsEqualIgnoringCase(name), value);
    }

    /**
     * Ctor.
     *
     * @param name Name matcher.
     * @param value Value matcher.
     */
    public IsHeader(final Matcher<String> name, final Matcher<String> value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.name)
            .appendText(" ")
            .appendDescriptionOf(this.value);
    }

    @Override
    public boolean matchesSafely(final Map.Entry<String, String> item) {
        return this.name.matches(item.getKey()) && this.value.matches(item.getValue());
    }
}
