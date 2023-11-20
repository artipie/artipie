/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.rq.RqHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import wtf.g4s8.mime.MimeType;

/**
 * Accept header, check
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept">documentation</a>
 * for more details.
 *
 * @since 0.19
 */
public final class Accept {

    /**
     * Header name.
     */
    public static final String NAME = "Accept";

    /**
     * Headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Ctor.
     * @param headers Headers to extract `accept` header from
     */
    public Accept(final Iterable<Map.Entry<String, String>> headers) {
        this.headers = headers;
    }

    /**
     * Parses `Accept` header values, sorts them according to weight and returns in
     * corresponding order.
     * @return Set or the values
     * @checkstyle ReturnCountCheck (11 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    public List<String> values() {
        final RqHeaders rqh = new RqHeaders(this.headers, Accept.NAME);
        if (rqh.size() == 0) {
            return Collections.emptyList();
        }
        return MimeType.parse(
            rqh.stream().collect(Collectors.joining(","))
        ).stream()
            .map(mime -> String.format("%s/%s", mime.type(), mime.subtype()))
            .collect(Collectors.toList());
    }
}
