/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;

/**
 * Response matcher.
 * @since 0.10
 */
public final class ResponseMatcher extends AllOf<Response> {

    /**
     * Ctor.
     *
     * @param status Expected status
     * @param headers Expected headers
     * @param body Expected body
     */
    public ResponseMatcher(
        final RsStatus status,
        final Iterable<? extends Map.Entry<String, String>> headers,
        final byte[] body
    ) {
        super(
            new RsHasStatus(status),
            new RsHasHeaders(headers),
            new RsHasBody(body)
        );
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param body Expected body
     * @param headers Expected headers
     */
    @SafeVarargs
    public ResponseMatcher(
        final RsStatus status,
        final byte[] body,
        final Map.Entry<String, String>... headers
    ) {
        super(
            new RsHasStatus(status),
            new RsHasHeaders(headers),
            new RsHasBody(body)
        );
    }

    /**
     * Ctor.
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
     * Ctor.
     * @param status Expected status
     * @param body Expected body
     */
    public ResponseMatcher(final RsStatus status, final String body) {
        this(
            status,
            body,
            StandardCharsets.UTF_8
        );
    }

    /**
     * Ctor.
     * @param body Expected body
     */
    public ResponseMatcher(final Matcher<String> body) {
        this(body, StandardCharsets.UTF_8);
    }

    /**
     * Ctor.
     * @param body Expected body
     */
    public ResponseMatcher(final String body) {
        this(Matchers.is(body));
    }

    /**
     * Ctor.
     * @param body Expected body
     * @param charset Character set
     */
    public ResponseMatcher(final Matcher<String> body, final Charset charset) {
        this(RsStatus.OK, body, charset);
    }

    /**
     * Ctor.
     * @param body Expected body
     * @param charset Character set
     */
    public ResponseMatcher(final String body, final Charset charset) {
        this(RsStatus.OK, body, charset);
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param body Expected body
     * @param charset Character set
     */
    public ResponseMatcher(final RsStatus status, final String body, final Charset charset) {
        this(
            status,
            Matchers.is(body),
            charset
        );
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param body Expected body
     * @param charset Character set
     */
    public ResponseMatcher(
        final RsStatus status,
        final Matcher<String> body,
        final Charset charset
    ) {
        super(
            new RsHasStatus(status),
            new RsHasBody(body, charset)
        );
    }

    /**
     * Ctor.
     * @param body Expected body
     */
    public ResponseMatcher(final byte[] body) {
        this(RsStatus.OK, body);
    }

    /**
     * Ctor.
     *
     * @param headers Expected headers
     */
    public ResponseMatcher(final Iterable<? extends Map.Entry<String, String>> headers) {
        this(
            RsStatus.OK,
            new RsHasHeaders(headers)
        );
    }

    /**
     * Ctor.
     * @param headers Expected headers
     */
    @SafeVarargs
    public ResponseMatcher(final Map.Entry<String, String>... headers) {
        this(
            RsStatus.OK,
            new RsHasHeaders(headers)
        );
    }

    /**
     * Ctor.
     *
     * @param status Expected status
     * @param headers Expected headers
     */
    public ResponseMatcher(
        final RsStatus status,
        final Iterable<? extends Map.Entry<String, String>> headers
    ) {
        this(
            status,
            new RsHasHeaders(headers)
        );
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param headers Expected headers
     */
    @SafeVarargs
    public ResponseMatcher(final RsStatus status, final Map.Entry<String, String>... headers) {
        this(
            status,
            new RsHasHeaders(headers)
        );
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param headers Matchers for expected headers
     */
    @SafeVarargs
    public ResponseMatcher(
        final RsStatus status,
        final Matcher<? super Map.Entry<String, String>>... headers
    ) {
        this(
            status,
            new RsHasHeaders(headers)
        );
    }

    /**
     * Ctor.
     * @param status Expected status
     * @param headers Matchers for expected headers
     */
    public ResponseMatcher(
        final RsStatus status,
        final Matcher<Response> headers
    ) {
        super(
            new RsHasStatus(status), headers
        );
    }

}
