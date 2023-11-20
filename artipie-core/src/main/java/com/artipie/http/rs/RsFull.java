/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Response;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * RsFull, response with status code, headers and body.
 *
 * @since 0.8
 */
public final class RsFull extends Response.Wrap {

    /**
     * Ctor.
     * @param status Status code
     * @param headers Headers
     * @param body Response body
     */
    public RsFull(
        final RsStatus status,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        this(status, headers, new Content.From(body));
    }

    /**
     * Ctor.
     * @param status Status code
     * @param headers Headers
     * @param body Response body
     */
    public RsFull(
        final RsStatus status,
        final Iterable<Map.Entry<String, String>> headers,
        final Content body) {
        super(
            new RsWithStatus(
                new RsWithHeaders(
                    new RsWithBody(
                        body
                    ), headers
                ), status
            )
        );
    }
}
