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
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

/**
 * Removes trailing slash chars from URI path.
 * @since 0.8
 */
public final class TrimSlice implements Slice {

    /**
     * Trailing slash pattern.
     */
    private static final Pattern PTN = Pattern.compile("/+$");

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Wraps slice to remove trailing slash chars.
     * @param origin Origin slice
     */
    public TrimSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RequestLineFrom rql = new RequestLineFrom(line);
        return this.origin.response(
            new RequestLine(
                rql.method().value(),
                new URIBuilder(rql.uri())
                    .setPath(trim(rql.uri().getPath()))
                    .toString(),
                rql.version()
            ).toString(),
            headers,
            body
        );
    }

    /**
     * Trim trailing slashes.
     * @param path Path
     * @return Trimmed path
     */
    private static String trim(final String path) {
        final String res;
        if (path.isEmpty() || "/".equals(path)) {
            res = path;
        } else {
            res = TrimSlice.PTN.matcher(path).replaceAll("");
        }
        return res;
    }
}
