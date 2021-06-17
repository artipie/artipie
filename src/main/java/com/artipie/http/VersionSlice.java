/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.ArtipieProperties;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Returns JSON with information about version of application.
 * @since 0.21
 */
public final class VersionSlice implements Slice {
    /**
     * Name of file with.
     */
    public static final String PROPERTIES_FILE = "artipie.properties";

    /**
     * Key of field which contains Artipie version.
     */
    public static final String VERSION_KEY = "artipie.version";

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> new RsWithStatus(
                    new RsJson(
                        Json.createArrayBuilder().add(
                            Json.createObjectBuilder().add(
                                "version",
                                new ArtipieProperties().version()
                            )
                        ).build()
                    ),
                    RsStatus.OK
                )
            )
        );
    }
}
