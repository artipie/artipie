/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;

/**
 * Decorator for {@link Slice} which adds headers to the origin.
 */
public final class SliceWithHeaders implements Slice {

    private final Slice origin;
    private final Headers additional;

    /**
     * @param origin Origin slice
     * @param headers Headers
     */
    public SliceWithHeaders(Slice origin, Headers headers) {
        this.origin = origin;
        this.additional = headers;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return origin.response(line, headers, body)
            .thenApply(
                res -> {
                    ResponseBuilder builder = ResponseBuilder.from(res.status())
                        .headers(res.headers())
                        .body(res.body());
                    additional.stream().forEach(h -> builder.header(h.getKey(), h.getValue()));
                    return builder.build();
                }
            );
    }
}
