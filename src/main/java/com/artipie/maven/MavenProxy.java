/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.maven.http.MavenProxySlice;
import com.artipie.repo.RepoConfig;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;

/**
 * Maven proxy slice created from config.
 *
 * @since 0.12
 */
public final class MavenProxy implements Slice {

    /**
     * HTTP client.
     */
    private final ClientSlices client;

    /**
     * Repository configuration.
     */
    private final RepoConfig cfg;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param cfg Repository configuration.
     */
    public MavenProxy(final ClientSlices client, final RepoConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new GroupSlice(
            this.cfg.proxy().remotes().stream().map(
                remote -> new MavenProxySlice(
                    this.client,
                    URI.create(remote.url()),
                    remote.auth(),
                    remote.cache().<Cache>map(
                        cache -> new FromStorageCache(cache.storage())
                    ).orElse(Cache.NOP)
                )
            ).collect(Collectors.toList())
        ).response(line, headers, body);
    }
}
