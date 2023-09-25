/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.adapters.file;

import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.files.FileProxySlice;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.proxy.ProxyConfig;
import com.artipie.settings.repo.proxy.YamlProxyConfig;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.reactivestreams.Publisher;

/**
 * File proxy slice created from config.
 *
 * @since 0.12
 */
public final class FileProxy implements Slice {

    /**
     * HTTP client.
     */
    private final ClientSlices client;

    /**
     * Repository configuration.
     */
    private final RepoConfig cfg;

    /**
     * Artifact events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param events Artifact events queue
     */
    public FileProxy(final ClientSlices client, final RepoConfig cfg,
        final Optional<Queue<ArtifactEvent>> events) {
        this.client = client;
        this.cfg = cfg;
        this.events = events;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Collection<? extends ProxyConfig.Remote> remotes =
            new YamlProxyConfig(this.client, this.cfg).remotes();
        if (remotes.isEmpty()) {
            throw new IllegalArgumentException("No remotes specified");
        }
        if (remotes.size() > 1) {
            throw new IllegalArgumentException("Only one remote is allowed");
        }
        final ProxyConfig.Remote remote = remotes.iterator().next();
        return new FileProxySlice(
            new AuthClientSlice(
                new UriClientSlice(this.client, URI.create(remote.url())), remote.auth()
            ),
            remote.cache().<Cache>map(cache -> new FromStorageCache(cache.storage()))
                .orElse(Cache.NOP),
            remote.cache().flatMap(ignored -> this.events),
            this.cfg.name()
        ).response(line, headers, body);
    }
}
