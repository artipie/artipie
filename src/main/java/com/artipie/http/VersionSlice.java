/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.ArtipieIOException;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Returns JSON with information about version of application.
 * @since 0.20
 */
public final class VersionSlice implements Slice {
    /**
     * Name of file with.
     */
    static final String PROPERTIES_FILE = ".properties";

    /**
     * Key of field which contains Artipie version.
     */
    static final String VERSION_KEY = "artipie.version";

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            CompletableFuture.supplyAsync(
                () -> {
                    final Properties properties = new Properties();
                    try {
                        properties.load(
                            Thread.currentThread()
                                .getContextClassLoader()
                                .getResourceAsStream(VersionSlice.PROPERTIES_FILE)
                        );
                        return new RsWithStatus(
                            new RsJson(
                                Json.createArrayBuilder().add(
                                    Json.createObjectBuilder().add(
                                        "version",
                                        properties.getProperty(VersionSlice.VERSION_KEY)
                                    )
                                ).build()
                            ),
                            RsStatus.OK
                        );
                    } catch (final IOException exc) {
                        throw new ArtipieIOException(exc);
                    }
                }
            )
        );
    }
}
