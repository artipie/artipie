/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;

import java.util.concurrent.CompletableFuture;

/**
 * Absent resource, sends HTTP 404 Not Found response to every request.
 */
public final class Absent implements Resource {

    @Override
    public CompletableFuture<ResponseImpl> get(final Headers headers) {
        return ResponseBuilder.notFound().completedFuture();
    }

    @Override
    public CompletableFuture<ResponseImpl> put(Headers headers, Content body) {
        return ResponseBuilder.notFound().completedFuture();
    }
}
