/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice to serve on `/authentication-type`, returns stab json body.
 * @since 0.4
 */
final class AuthTypeSlice implements Slice {

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new RsJson(
            () -> Json.createObjectBuilder().add("authentication_type", "password").build(),
            StandardCharsets.UTF_8
        );
    }
}
