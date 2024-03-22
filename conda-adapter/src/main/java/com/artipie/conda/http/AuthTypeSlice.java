/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.BaseResponse;

import javax.json.Json;

/**
 * Slice to serve on `/authentication-type`, returns stab json body.
 */
final class AuthTypeSlice implements Slice {
    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return BaseResponse.ok()
            .jsonBody(Json.createObjectBuilder().add("authentication_type", "password").build());
    }
}
