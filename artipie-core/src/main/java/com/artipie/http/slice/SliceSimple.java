/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Simple decorator for Slice.
 */
public final class SliceSimple implements Slice {

    private final Supplier<Response> res;

    public SliceSimple(Response response) {
        this.res = () -> response;
    }

    public SliceSimple(Supplier<Response> res) {
        this.res = res;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        return CompletableFuture.completedFuture(this.res.get());
    }
}
