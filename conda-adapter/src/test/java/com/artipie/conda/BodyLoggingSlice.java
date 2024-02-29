/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.Content;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.jcabi.log.Logger;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Slice decorator to log request body.
 */
final class BodyLoggingSlice implements Slice {

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
     * @param origin Origin slice
     */
    BodyLoggingSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new Content.From(body).asBytesFuture().thenApply(
                bytes -> {
                    Logger.debug(this.origin, new String(bytes, StandardCharsets.UTF_8));
                    return this.origin.response(line, headers, new Content.From(bytes));
                }
            )
        );
    }
}
