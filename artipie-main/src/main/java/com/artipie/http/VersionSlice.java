/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.common.RsJson;
import com.artipie.misc.ArtipieProperties;
import java.nio.ByteBuffer;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Returns JSON with information about version of application.
 * @since 0.21
 */
public final class VersionSlice implements Slice {
    /**
     * Artipie properties.
     */
    private final ArtipieProperties properties;

    /**
     * Ctor.
     * @param properties Artipie properties
     */
    public VersionSlice(final ArtipieProperties properties) {
        this.properties = properties;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsJson(
            Json.createArrayBuilder().add(
                Json.createObjectBuilder().add("version", this.properties.version())
            ).build()
        );
    }
}
