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
import com.artipie.http.rs.common.RsJson;

import javax.json.Json;
import java.nio.charset.StandardCharsets;

/**
 * Slice to serve on `/authentication-type`, returns stab json body.
 * @since 0.4
 */
final class AuthTypeSlice implements Slice {

    @Override
    public Response response(RequestLine line, Headers headers,
                             Content body) {
        return new RsJson(
            () -> Json.createObjectBuilder().add("authentication_type", "password").build(),
            StandardCharsets.UTF_8
        );
    }
}
