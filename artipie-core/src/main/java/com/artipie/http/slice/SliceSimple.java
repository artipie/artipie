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

/**
 * Simple decorator for Slice.
 */
public final class SliceSimple implements Slice {

    /**
     * Response.
     */
    private final Response res;

    /**
     * Response.
     * @param response Response.
     */
    public SliceSimple(final Response response) {
        this.res = response;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return this.res;
    }
}
