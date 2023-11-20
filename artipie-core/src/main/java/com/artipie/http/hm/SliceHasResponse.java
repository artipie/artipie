/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.CachedResponse;
import io.reactivex.Flowable;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link Slice} response.
 * @since 0.16
 */
public final class SliceHasResponse extends TypeSafeMatcher<Slice> {

    /**
     * Response matcher.
     */
    private final Matcher<? extends Response> rsp;

    /**
     * Function to get response from slice.
     */
    private final Function<? super Slice, ? extends Response> responser;

    /**
     * Response cache.
     */
    private Response rcache;

    /**
     * New response matcher for slice with request line.
     * @param rsp Response matcher
     * @param line Request line
     */
    public SliceHasResponse(final Matcher<? extends Response> rsp, final RequestLine line) {
        this(rsp, line, Headers.EMPTY, new Content.From(Flowable.empty()));
    }

    /**
     * New response matcher for slice with request line, headers and body.
     * @param rsp Response matcher
     * @param line Request line
     * @param headers Headers
     * @param body Body
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public SliceHasResponse(final Matcher<? extends Response> rsp, final RequestLine line,
        final Headers headers, final Content body) {
        this.rsp = rsp;
        this.responser = slice -> slice.response(line.toString(), headers, body);
    }

    @Override
    public boolean matchesSafely(final Slice item) {
        return this.rsp.matches(this.response(item));
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("response: ").appendDescriptionOf(this.rsp);
    }

    @Override
    public void describeMismatchSafely(final Slice item, final Description description) {
        description.appendText("response was: ").appendValue(this.response(item));
    }

    /**
     * Response for slice.
     * @param slice Target slice
     * @return Cached response
     */
    private Response response(final Slice slice) {
        if (this.rcache == null) {
            this.rcache = new CachedResponse(this.responser.apply(slice));
        }
        return this.rcache;
    }
}
