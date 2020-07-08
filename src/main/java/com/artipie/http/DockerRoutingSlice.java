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
package com.artipie.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URIBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice decorator which redirects all Docker V2 API requests to Artipie format paths.
 * @since 0.9
 */
final class DockerRoutingSlice implements Slice {

    /**
     * Docker V2 API path pattern.
     */
    private static final Pattern PTN_PATH = Pattern.compile("/v2((/.*)?)");

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Decorates slice with Docker V2 API routing.
     * @param origin Origin slice
     */
    DockerRoutingSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final RequestLineFrom req = new RequestLineFrom(line);
        final Matcher matcher = PTN_PATH.matcher(req.uri().getPath());
        final Response rsp;
        if (matcher.matches()) {
            rsp = this.origin.response(
                new RequestLine(
                    req.method().toString(),
                    new URIBuilder(req.uri()).setPath(matcher.group(1)).toString(),
                    req.version()
                ).toString(),
                headers, body
            );
        } else {
            rsp = this.origin.response(line, headers, body);
        }
        return rsp;
    }
}
