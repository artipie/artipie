/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice created from {@link Resource}.
 *
 * @since 0.2
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
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        final RqMethod method = new RequestLineFrom(line).method();
        if (method.equals(RqMethod.GET)) {
            response = this.origin.get(new Headers.From(headers));
        } else if (method.equals(RqMethod.PUT)) {
            response = this.origin.put(new Headers.From(headers), body);
        } else {
            response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
        return response;
    }
}
