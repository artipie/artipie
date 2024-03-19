/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.regex.Pattern;

/**
 * Slice handles routing paths. It removes predefined routing path and passes the rest part
 * to the underlying slice.
 *
 * @since 0.6
 */
public final class ReplacePathSlice implements Slice {
    /**
     * Routing path.
     */
    private final String path;

    /**
     * Underlying slice.
     */
    private final Slice original;

    /**
     * Ctor.
     * @param path Routing path ("/" for ROOT context)
     * @param original Underlying slice
     */
    public ReplacePathSlice(final String path, final Slice original) {
        this.path = path;
        this.original = original;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body) {
        return this.original.response(
            new RequestLine(
                line.method().value(),
                String.format(
                    "/%s",
                    line.uri().getPath().replaceFirst(
                        String.format("%s/?", Pattern.quote(this.path)),
                        ""
                    )
                ),
                line.version()
            ),
            headers,
            body
        );
    }
}
