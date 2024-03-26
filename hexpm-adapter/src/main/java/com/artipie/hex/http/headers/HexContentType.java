/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;

import java.util.Map;

/**
 * ContentType header for HexPm.
 */
public class HexContentType {
    /**
     * Default ContentType.
     */
    static final String DEFAULT_TYPE = "application/vnd.hex+erlang";

    /**
     * Request headers.
     */
    private final Headers headers;

    /**
     * @param headers Request headers.
     */
    public HexContentType(Headers headers) {
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
        return this.headers.copy().add(ContentType.mime(type));
    }
}
