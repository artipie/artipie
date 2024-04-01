/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Slice created from {@link Resource}.
 */
final class SliceFromResource implements Slice {

    /**
     * Origin resource.
     */
    private final Resource origin;

    /**
     * @param origin Origin resource.
     */
    SliceFromResource(final Resource origin) {
        this.origin = origin;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        final RqMethod method = line.method();
        if (method.equals(RqMethod.GET)) {
            return this.origin.get(headers);
        }
        if (method.equals(RqMethod.PUT)) {
            return this.origin.put(headers, body);
        }
        return ResponseBuilder.methodNotAllowed().completedFuture();
    }
}
