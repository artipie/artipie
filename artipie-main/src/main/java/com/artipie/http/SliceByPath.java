/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.RqPath;
import com.artipie.asto.Key;
import com.artipie.http.auth.Tokens;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.settings.Settings;
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
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Tokens: authentication and generation.
     */
    private final Tokens tokens;

    /**
     * New slice from settings.
     *
     * @param settings Artipie settings
     * @param tokens Tokens: authentication and generation
     */
    SliceByPath(
        final Settings settings,
        final Tokens tokens
    ) {
        this.settings = settings;
        this.tokens = tokens;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<Key> key = SliceByPath.keyFromPath(
            new RequestLineFrom(line).uri().getPath()
        );
        if (key.isEmpty()) {
            return new RsWithBody(
                new RsWithStatus(RsStatus.NOT_FOUND),
                "Failed to find a repository",
                StandardCharsets.UTF_8
            );
        }
        return new ArtipieRepositories(this.settings, this.tokens)
            .slice(key.get(), new RequestLineFrom(line).uri().getPort())
            .response(line, headers, body);
    }

    /**
     * Repository key from path.
     * @param path Path to get repository key from
     * @return Key if found
     */
    private static Optional<Key> keyFromPath(final String path) {
        final String[] parts = SliceByPath.splitPath(path);
        final Optional<Key> key;
        if (RqPath.CONDA.test(path)) {
            key = Optional.of(new Key.From(parts[2]));
        } else if (parts.length >= 1 && !parts[0].isBlank()) {
            key = Optional.of(new Key.From(parts[0]));
        } else {
            key = Optional.empty();
        }
        return key;
    }

    /**
     * Split path into parts.
     *
     * @param path Path.
     * @return Array of path parts.
     */
    private static String[] splitPath(final String path) {
        return path.replaceAll("^/+", "").split("/");
    }
}
