/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.slice;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Simple decorator for Slice.
 *
 * @since 0.7
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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.res;
    }
}
