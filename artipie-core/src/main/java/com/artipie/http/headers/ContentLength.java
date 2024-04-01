/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;

/**
 * Content-Length header.
 */
public final class ContentLength extends Header {

    public static Header with(long size) {
        return new ContentLength(String.valueOf(size));
    }

    /**
     * Header name.
     */
    public static final String NAME = "Content-Length";

    /**
     * @param length Length number
     */
    public ContentLength(final Number length) {
        this(length.toString());
    }

    /**
     * @param value Header value.
     */
    public ContentLength(final String value) {
        super(new Header(ContentLength.NAME, value));
    }

    /**
     * @param headers Headers to extract header from.
     */
    public ContentLength(final Headers headers) {
        this(headers.single(ContentLength.NAME).getValue());
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
