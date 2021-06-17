/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.php;

import com.artipie.RepoConfig;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.http.proxy.ComposerProxySlice;
import com.artipie.composer.http.proxy.ComposerStorageCache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.repo.ProxyConfig;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Php Composer proxy slice.
 * @since 0.20
 */
public final class ComposerProxy implements Slice {
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
     * @param client HTTP client
     * @param cfg Repository configuration
     */
    public ComposerProxy(final ClientSlices client, final RepoConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Collection<? extends ProxyConfig.Remote> remotes = this.cfg.proxy().remotes();
        if (remotes.isEmpty()) {
            throw new IllegalArgumentException("No remotes were specified");
        }
        if (remotes.size() > 1) {
            throw new IllegalArgumentException("Only one remote is allowed");
        }
        final ProxyConfig.Remote remote = remotes.iterator().next();
        return remote.cache().map(
            storage -> new ComposerProxySlice(
                this.client,
                URI.create(remote.url()),
                new AstoRepository(this.cfg.storage()),
                remote.auth(),
                new ComposerStorageCache(new AstoRepository(storage))
            )
        ).orElseGet(
            () -> new ComposerProxySlice(
                this.client,
                URI.create(remote.url()),
                new AstoRepository(this.cfg.storage()),
                remote.auth()
            )
        ).response(line, headers, body);
    }
}
