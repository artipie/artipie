/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.KeyFromPath;

import java.util.concurrent.CompletableFuture;

public final class DeleteSlice implements Slice {
    private final Storage asto;

    public DeleteSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        final Key key = new KeyFromPath(line.uri().getPath());


        return this.asto.exists(key).thenCompose(
                exists -> {
                    if (exists) {
                        return this.asto.delete(key).thenApply(
                                nothing -> ResponseBuilder.ok().build()
                        ).toCompletableFuture();
                    } else {
                        return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
                    }
                }
        );
    }
}
