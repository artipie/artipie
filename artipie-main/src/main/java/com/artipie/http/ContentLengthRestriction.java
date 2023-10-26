/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice limiting requests size by `Content-Length` header.
 * Checks `Content-Length` header to be within limit and responds with error if it is not.
 * Forwards request to delegate {@link Slice} otherwise.
 *
 * @since 0.2
 */
public final class ContentLengthRestriction implements Slice {

    /**
     * Delegate slice.
     */
    private final Slice delegate;

    /**
     * Max allowed value.
     */
    private final long limit;

    /**
     * Ctor.
     *
     * @param delegate Delegate slice.
     * @param limit Max allowed value.
     */
    public ContentLengthRestriction(final Slice delegate, final long limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        if (new RqHeaders(headers, "Content-Length").stream().allMatch(this::withinLimit)) {
            response = this.delegate.response(line, headers, body);
        } else {
            response = new RsWithStatus(RsStatus.PAYLOAD_TOO_LARGE);
        }
        return response;
    }

    /**
     * Checks that value is less or equal then limit.
     *
     * @param value Value to check against limit.
     * @return True if value is within limit or cannot be parsed, false otherwise.
     */
    private boolean withinLimit(final String value) {
        boolean pass;
        try {
            pass = Long.parseLong(value) <= this.limit;
        } catch (final NumberFormatException ex) {
            pass = true;
        }
        return pass;
    }
}
