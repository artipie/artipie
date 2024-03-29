/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.php;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.http.proxy.ComposerProxySlice;
import com.artipie.composer.http.proxy.ComposerStorageCache;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.RemoteConfig;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.rq.RequestLine;
import com.artipie.settings.repo.RepoConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Php Composer proxy slice.
 */
public final class ComposerProxy implements Slice {

    private final Slice slice;

    /**
     * @param client HTTP client
     * @param cfg Repository configuration
     */
    public ComposerProxy(ClientSlices client, RepoConfig cfg) {
        final RemoteConfig remote = cfg.remoteConfig();
        final Optional<Storage> asto = cfg.storageOpt();
        slice = asto.map(
            cache -> new ComposerProxySlice(
                client,
                remote.uri(),
                new AstoRepository(cfg.storage()),
                GenericAuthenticator.create(client, remote.username(), remote.pwd()),
                new ComposerStorageCache(new AstoRepository(cache))
            )
        ).orElseGet(
            () -> new ComposerProxySlice(
                client,
                remote.uri(),
                new AstoRepository(cfg.storage()),
                GenericAuthenticator.create(client, remote.username(), remote.pwd())
            )
        );
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        RequestLine line,
        Headers headers,
        Content body
    ) {
        return slice.response(line, headers, body);
    }
}
