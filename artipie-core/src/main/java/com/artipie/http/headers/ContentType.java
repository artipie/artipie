/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;

/**
 * Content-Type header.
 *
 * @since 0.11
 */
public final class ContentType extends Header.Wrap {

    /**
     * Header name.
     */
    public static final String NAME = "Content-Type";

    /**
     * Ctor.
     *
     * @param value Header value.
     */
    public ContentType(final String value) {
        super(new Header(ContentType.NAME, value));
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public ContentType(final Headers headers) {
        this(new RqHeaders.Single(headers, ContentType.NAME).asString());
    }
}
