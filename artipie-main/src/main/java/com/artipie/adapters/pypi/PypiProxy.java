/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.pypi;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.RemoteConfig;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.rq.RequestLine;
import com.artipie.pypi.http.PyProxySlice;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.settings.repo.RepoConfig;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;

/**
 * Pypi proxy slice.
 */
public final class PypiProxy implements Slice {

    private final Slice slice;

    /**
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param queue Artifact events queue
     */
    public PypiProxy(
        ClientSlices client,
        RepoConfig cfg,
        Optional<Queue<ProxyArtifactEvent>> queue
    ) {
        final RemoteConfig remote = cfg.remoteConfig();
        slice = new PyProxySlice(
            client, remote.uri(),
            GenericAuthenticator.create(client, remote.username(), remote.pwd()),
            cfg.storageOpt().orElseThrow(
                () -> new IllegalStateException("Python proxy requires proxy storage to be set")
            ), queue, cfg.name()
        );
    }

    @Override
    public Response response(
        final RequestLine line,
        final Headers headers,
        final Publisher<ByteBuffer> body
    ) {
        return slice.response(line, headers, body);
    }
}
