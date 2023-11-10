/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.hex.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import java.util.Map;

/**
 * ContentType header for HexPm.
 *
 * @since 0.2
 */
public class HexContentType {
    /**
     * Default ContentType.
     */
    static final String DEFAULT_TYPE = "application/vnd.hex+erlang";

    /**
     * Request headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Ctor.
     *
     * @param headers Request headers.
     */
    public HexContentType(final Iterable<Map.Entry<String, String>> headers) {
        this.headers = headers;
    }

    /**
     * Fill ContentType header for response.
     *
     * @return Filled headers.
     */
    public Headers fill() {
        String type = HexContentType.DEFAULT_TYPE;
        for (final Map.Entry<String, String> header : this.headers) {
            if (Accept.NAME.equalsIgnoreCase(header.getKey()) && !header.getValue().isEmpty()) {
                type = header.getValue();
            }
        }
        return new Headers.From(this.headers, new ContentType(type));
    }
}
