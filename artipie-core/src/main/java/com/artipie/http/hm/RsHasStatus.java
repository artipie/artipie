/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.hm;

import com.artipie.http.ResponseImpl;
import com.artipie.http.RsStatus;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

/**
 * Matcher to verify response status.
 */
public final class RsHasStatus extends TypeSafeMatcher<ResponseImpl> {

    /**
     * Status code matcher.
     */
    private final Matcher<RsStatus> status;

    /**
     * @param status Code to match
     */
    public RsHasStatus(final RsStatus status) {
        this(new IsEqual<>(status));
    }

    /**
     * @param status Code matcher
     */
    public RsHasStatus(final Matcher<RsStatus> status) {
        this.status = status;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendDescriptionOf(this.status);
    }

    @Override
    public boolean matchesSafely(final ResponseImpl item) {
        return this.status.matches(item.status());
    }
}
