/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;

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
    public RsFull(RsStatus status, Headers headers, Publisher<ByteBuffer> body) {
        this(status, headers, new Content.From(body));
    }

    /**
     * Ctor.
     * @param status Status code
     * @param headers Headers
     * @param body Response body
     */
    public RsFull(RsStatus status, Headers headers, Content body) {
        super(
            new RsWithStatus(
                new RsWithHeaders(
                    BaseResponse.ok().body(body), headers
                ), status
            )
        );
    }
}
