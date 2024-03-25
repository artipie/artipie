/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.RepositorySlices;
import com.artipie.RqPath;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.rq.RequestLine;

import java.util.Optional;

/**
 * Slice which finds repository by path.
 */
final class SliceByPath implements Slice {

    /**
     * Slices cache.
     */
    private final RepositorySlices slices;

    /**
     * Create SliceByPath.
     *
     * @param slices Slices cache
     */
    SliceByPath(final RepositorySlices slices) {
        this.slices = slices;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        final Optional<Key> key = SliceByPath.keyFromPath(line.uri().getPath());
        if (key.isEmpty()) {
            return ResponseBuilder.notFound()
                .textBody("Failed to find a repository")
                .build();
        }
        return this.slices.slice(key.get(), line.uri().getPort())
            .response(line, headers, body);
    }

    /**
     * Repository key from path.
     * @param path Path to get repository key from
     * @return Key if found
     */
    private static Optional<Key> keyFromPath(final String path) {
        final String[] parts = SliceByPath.splitPath(path);
        if (RqPath.CONDA.test(path)) {
            return Optional.of(new Key.From(parts[2]));
        }
        if (parts.length >= 1 && !parts[0].isBlank()) {
            return Optional.of(new Key.From(parts[0]));
        }
        return Optional.empty();
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
