/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsWithHeaders;

/**
 * Decorator for {@link Slice} which adds headers to the origin.
 */
public final class SliceWithHeaders implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Headers.
     */
    private final Headers headers;

    /**
     * Ctor.
     * @param origin Origin slice
     * @param headers Headers
     */
    public SliceWithHeaders(final Slice origin, final Headers headers) {
        this.origin = origin;
        this.headers = headers;
    }

    @Override
    public Response response(RequestLine line, Headers hdrs, Content body) {
        return new RsWithHeaders(
            this.origin.response(line, hdrs, body),
            this.headers
        );
    }
}
