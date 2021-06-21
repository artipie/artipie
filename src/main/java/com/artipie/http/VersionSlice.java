/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.ArtipieProperties;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Map;
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
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsJson(
            Json.createArrayBuilder().add(
                Json.createObjectBuilder().add("version", this.properties.version())
            ).build()
        );
    }
}
