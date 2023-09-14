/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.adapters.maven;

import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.maven.http.MavenProxySlice;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.proxy.YamlProxyConfig;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
     * Artifact metadata events queue.
     */
    private final Optional<Queue<ProxyArtifactEvent>> queue;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param queue Artifact events queue
     */
    public MavenProxy(final ClientSlices client, final RepoConfig cfg,
        final Optional<Queue<ProxyArtifactEvent>> queue) {
        this.client = client;
        this.cfg = cfg;
        this.queue = queue;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new GroupSlice(
            new YamlProxyConfig(this.client, this.cfg).remotes().stream().map(
                remote -> new MavenProxySlice(
                    this.client,
                    URI.create(remote.url()),
                    remote.auth(),
                    remote.cache().<Cache>map(
                        cache -> new FromStorageCache(cache.storage())
                    ).orElse(Cache.NOP),
                    this.queue,
                    this.cfg.name()
                )
            ).collect(Collectors.toList())
        ).response(line, headers, body);
    }
}
