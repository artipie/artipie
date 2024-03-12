/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.maven;

import com.artipie.asto.Storage;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.group.GroupSlice;
import com.artipie.maven.http.MavenProxySlice;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.settings.repo.RepoConfig;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * Maven proxy slice created from config.
 */
public final class MavenProxy implements Slice {

    private final Slice slice;

    /**
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param queue Artifact events queue
     */
    public MavenProxy(
        ClientSlices client, RepoConfig cfg, Optional<Queue<ProxyArtifactEvent>> queue
    ) {
        final Optional<Storage> asto = cfg.storageOpt();
        slice = new GroupSlice(
            cfg.remotes().stream().map(
                remote -> new MavenProxySlice(
                    client, remote.uri(),
                    GenericAuthenticator.create(client, remote.username(), remote.pwd()),
                    asto.<Cache>map(FromStorageCache::new).orElse(Cache.NOP),
                    asto.flatMap(ignored -> queue),
                    cfg.name()
                )
            ).collect(Collectors.toList())
        );
    }

    @Override
    public Response response(
        String line,
        Iterable<Map.Entry<String, String>> headers,
        Publisher<ByteBuffer> body
    ) {
        return slice.response(line, headers, body);
    }
}
