/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;

/**
 * Content-Length header.
 *
 * @since 0.10
 */
public final class ContentLength extends Header.Wrap {

    /**
     * Header name.
     */
    public static final String NAME = "Content-Length";

    /**
     * Ctor.
     * @param length Length number
     */
    public ContentLength(final Number length) {
        this(length.toString());
    }

    /**
     * Ctor.
     *
     * @param value Header value.
     */
    public ContentLength(final String value) {
        super(new Header(ContentLength.NAME, value));
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public ContentLength(final Headers headers) {
        this(new RqHeaders.Single(headers, ContentLength.NAME).asString());
    }

    /**
     * Read header as long value.
     *
     * @return Header value.
     */
    public long longValue() {
        return Long.parseLong(this.getValue());
    }
}
