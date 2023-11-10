/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

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
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RequestLineFrom request = new RequestLineFrom(line);
        return this.original.response(
            new RequestLine(
                request.method().value(),
                String.format(
                    "/%s",
                    request.uri().getPath().replaceFirst(
                        String.format("%s/?", Pattern.quote(this.path)),
                        ""
                    )
                ),
                request.version()
            ).toString(),
            headers,
            body
        );
    }
}
