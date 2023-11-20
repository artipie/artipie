/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import com.artipie.http.Headers;
import java.util.Map;

/**
 * Path prefix obtained from X-FullPath header and request line.
 * @since 0.16
 */
public final class RequestLinePrefix {

    /**
     * Full path header name.
     */
    private static final String HDR_FULL_PATH = "X-FullPath";

    /**
     * Request line.
     */
    private final String line;

    /**
     * Headers.
     */
    private final Headers headers;

    /**
     * Ctor.
     * @param line Request line
     * @param headers Request headers
     */
    public RequestLinePrefix(final String line, final Headers headers) {
        this.line = line;
        this.headers = headers;
    }

    /**
     * Ctor.
     * @param line Request line
     * @param headers Request headers
     */
    public RequestLinePrefix(final String line, final Iterable<Map.Entry<String, String>> headers) {
        this(line, new Headers.From(headers));
    }

    /**
     * Obtains path prefix by `X-FullPath` header and request line. If header is absent, empty line
     * is returned.
     * @return Path prefix
     */
    public String get() {
        return new RqHeaders(this.headers, RequestLinePrefix.HDR_FULL_PATH).stream()
            .findFirst()
            .map(
                item -> {
                    final String res;
                    final String first = this.line.replaceAll("^/", "").replaceAll("/$", "")
                        .split("/")[0];
                    if (item.indexOf(first) > 0) {
                        res = item.substring(0, item.indexOf(first) - 1);
                    } else {
                        res = item;
                    }
                    return res;
                }
            ).orElse("");
    }
}
