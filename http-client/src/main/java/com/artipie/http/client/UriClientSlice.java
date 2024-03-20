/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.net.URI;

import com.artipie.http.rq.RequestLine;

/**
 * Client slice that sends requests to host and port using scheme specified in URI.
 * If URI contains path then it is used as prefix. Other URI components are ignored.
 *
 * @since 0.3
 */
public final class UriClientSlice implements Slice {

    /**
     * Client slices.
     */
    private final ClientSlices client;

    /**
     * URI.
     */
    private final URI uri;

    /**
     * Ctor.
     *
     * @param client Client slices.
     * @param uri URI.
     */
    public UriClientSlice(final ClientSlices client, final URI uri) {
        this.client = client;
        this.uri = uri;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final Slice slice;
        final String path = this.uri.getRawPath();
        if (path == null) {
            slice = this.base();
        } else {
            slice = new PathPrefixSlice(this.base(), path);
        }
        return slice.response(line, headers, body);
    }

    /**
     * Get base client slice by scheme, host and port of URI ignoring path.
     *
     * @return Client slice.
     */
    private Slice base() {
        final Slice slice;
        final String scheme = this.uri.getScheme();
        final String host = this.uri.getHost();
        final int port = this.uri.getPort();
        switch (scheme) {
            case "https":
                if (port > 0) {
                    slice = this.client.https(host, port);
                } else {
                    slice = this.client.https(host);
                }
                break;
            case "http":
                if (port > 0) {
                    slice = this.client.http(host, port);
                } else {
                    slice = this.client.http(host);
                }
                break;
            default:
                throw new IllegalStateException(
                    String.format("Scheme '%s' is not supported", scheme)
                );
        }
        return slice;
    }
}
