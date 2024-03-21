/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.misc.ArtipieProperties;

import javax.json.Json;

/**
 * Returns JSON with information about version of application.
 */
public final class VersionSlice implements Slice {

    private final ArtipieProperties properties;

    public VersionSlice(final ArtipieProperties properties) {
        this.properties = properties;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return BaseResponse.ok()
            .jsonBody(Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("version", this.properties.version()))
                .build()
            );
    }
}
