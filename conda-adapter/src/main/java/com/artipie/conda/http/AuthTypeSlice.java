/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.ResponseBuilder;

import javax.json.Json;
import java.util.concurrent.CompletableFuture;

/**
 * Slice to serve on `/authentication-type`, returns stab json body.
 */
final class AuthTypeSlice implements Slice {
    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return ResponseBuilder.ok()
            .jsonBody(Json.createObjectBuilder().add("authentication_type", "password").build())
            .completedFuture();
    }
}
