/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.rq.RequestLine;
import com.artipie.misc.ArtipieProperties;

import javax.json.Json;
import java.util.concurrent.CompletableFuture;

/**
 * Returns JSON with information about version of application.
 */
public final class VersionSlice implements Slice {

    private final ArtipieProperties properties;

    public VersionSlice(final ArtipieProperties properties) {
        this.properties = properties;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        return ResponseBuilder.ok()
            .jsonBody(Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("version", this.properties.version()))
                .build()
            ).completedFuture();
    }
}
