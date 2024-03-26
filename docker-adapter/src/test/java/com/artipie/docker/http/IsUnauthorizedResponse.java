/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Matcher for unauthorized error response.
 */
public final class IsUnauthorizedResponse extends BaseMatcher<Response> {

    private final Matcher<Response> delegate;

    public IsUnauthorizedResponse() {
        this.delegate = new IsErrorsResponse(RsStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    @Override
    public boolean matches(final Object actual) {
        return this.delegate.matches(actual);
    }

    @Override
    public void describeTo(final Description description) {
        this.delegate.describeTo(description);
    }
}
