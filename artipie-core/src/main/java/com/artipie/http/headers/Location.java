/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;

/**
 * Location header.
 *
 * @since 0.11
 */
public final class Location extends Header.Wrap {

    /**
     * Header name.
     */
    public static final String NAME = "Location";

    /**
     * Ctor.
     *
     * @param value Header value.
     */
    public Location(final String value) {
        super(new Header(Location.NAME, value));
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public Location(final Headers headers) {
        this(new RqHeaders.Single(headers, Location.NAME).asString());
    }
}
