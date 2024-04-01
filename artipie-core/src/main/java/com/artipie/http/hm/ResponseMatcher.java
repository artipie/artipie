/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.Header;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;

/**
 * Response matcher.
 */
public final class ResponseMatcher extends AllOf<Response> {

    /**
     * @param status Expected status
     * @param headers Expected headers
     * @param body Expected body
     */
    public ResponseMatcher(
        final RsStatus status,
        final Iterable<? extends Header> headers,
        final byte[] body
    ) {
        super(
            new RsHasStatus(status),
            new RsHasHeaders(headers),
            new RsHasBody(body)
        );
    }

    /**
     * @param status Expected status
     * @param body Expected body
     * @param headers Expected headers
     */
    public ResponseMatcher(
        final RsStatus status,
        final byte[] body,
        final Header... headers
    ) {
        super(
            new RsHasStatus(status),
            new RsHasHeaders(headers),
            new RsHasBody(body)
        );
    }

    /**
     * @param status Expected status
     * @param body Expected body
     */
    public ResponseMatcher(final RsStatus status, final byte[] body) {
        super(
            new RsHasStatus(status),
            new RsHasBody(body)
        );
    }

    /**
     * @param body Expected body
     */
    public ResponseMatcher(final byte[] body) {
        this(RsStatus.OK, body);
    }

    /**
     * @param headers Expected headers
     */
    public ResponseMatcher(Iterable<? extends Header> headers) {
        this(RsStatus.OK, new RsHasHeaders(headers));
    }

    /**
     * @param headers Expected headers
     */
    public ResponseMatcher(Header... headers) {
        this(RsStatus.OK, new RsHasHeaders(headers));
    }

    /**
     * @param status Expected status
     * @param headers Expected headers
     */
    public ResponseMatcher(RsStatus status, Iterable<? extends Header> headers) {
        this(status, new RsHasHeaders(headers));
    }

    /**
     * @param status Expected status
     * @param headers Expected headers
     */
    public ResponseMatcher(RsStatus status, Header... headers) {
        this(status, new RsHasHeaders(headers));
    }

    /**
     * @param status Expected status
     * @param headers Matchers for expected headers
     */
    @SafeVarargs
    public ResponseMatcher(RsStatus status, Matcher<? super Header>... headers) {
        this(status, new RsHasHeaders(headers));
    }

    /**
     * @param status Expected status
     * @param headers Matchers for expected headers
     */
    public ResponseMatcher(RsStatus status, Matcher<Response> headers) {
        super(new RsHasStatus(status), headers);
    }
}
