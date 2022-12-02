/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Key;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.settings.Settings;
import com.artipie.settings.cache.StoragesCache;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Slice which finds repository by path.
 * @since 0.9
 */
final class SliceByPath implements Slice {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Storages cache.
     */
    private final StoragesCache cache;

    /**
     * New slice from settings.
     *
     * @param http HTTP client
     * @param settings Artipie settings
     * @param cache Storages cache
     */
    SliceByPath(
        final ClientSlices http,
        final Settings settings,
        final StoragesCache cache
    ) {
        this.http = http;
        this.settings = settings;
        this.cache = cache;
    }

    // @checkstyle ReturnCountCheck (20 lines)
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<Key> key = this.settings.layout().keyFromPath(
            new RequestLineFrom(line).uri().getPath()
        );
        if (key.isEmpty()) {
            return new RsWithBody(
                new RsWithStatus(RsStatus.NOT_FOUND),
                "Failed to find a repository",
                StandardCharsets.UTF_8
            );
        }
        return new ArtipieRepositories(this.http, this.settings, this.cache)
            .slice(key.get(), new RequestLineFrom(line).uri().getPort())
            .response(line, headers, body);
    }
}
