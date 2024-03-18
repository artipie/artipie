/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;

/**
 * Slice created from {@link Resource}.
 */
final class SliceFromResource implements Slice {

    /**
     * Origin resource.
     */
    private final Resource origin;

    /**
     * Ctor.
     *
     * @param origin Origin resource.
     */
    SliceFromResource(final Resource origin) {
        this.origin = origin;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        final RqMethod method = line.method();
        if (method.equals(RqMethod.GET)) {
            response = this.origin.get(headers);
        } else if (method.equals(RqMethod.PUT)) {
            response = this.origin.put(headers, body);
        } else {
            response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
        return response;
    }
}
