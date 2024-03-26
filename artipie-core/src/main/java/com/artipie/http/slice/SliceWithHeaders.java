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

    private final Slice origin;
    private final Headers headers;

    /**
     * @param origin Origin slice
     * @param headers Headers
     */
    public SliceWithHeaders(Slice origin, Headers headers) {
        this.origin = origin;
        this.headers = headers;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return new RsWithHeaders(origin.response(line, headers, body), this.headers);
    }
}
