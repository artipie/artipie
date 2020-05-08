/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.http.Response;
import com.artipie.http.Slice;
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
public final class ContentLengthLimiter implements Slice {

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
    public ContentLengthLimiter(final Slice delegate, final long limit) {
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
            response = new RsWithStatus(RsStatus.BAD_REQUEST);
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
