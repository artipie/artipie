/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Slice that forwards all requests to origin slice prepending path with specified prefix.
 *
 * @since 0.3
 */
public final class PathPrefixSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Prefix.
     */
    private final String prefix;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param prefix Prefix.
     */
    public PathPrefixSlice(final Slice origin, final String prefix) {
        this.origin = origin;
        this.prefix = prefix;
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final URI original = line.uri();
        final String uri;
        if (original.getRawQuery() == null) {
            uri = String.format("%s%s", this.prefix, original.getRawPath());
        } else {
            uri = String.format(
                "%s%s?%s",
                this.prefix,
                original.getRawPath(),
                original.getRawQuery()
            );
        }
        return this.origin.response(
            new RequestLine(line.method().value(), uri, line.version()),
            headers,
            body
        );
    }
}
