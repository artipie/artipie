/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Proxy {@link Docker} implementation.
 */
public final class ProxyDocker implements Docker {

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     */
    public ProxyDocker(final Slice remote) {
        this.remote = remote;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new ProxyRepo(this.remote, name);
    }

    @Override
    public CompletableFuture<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return new ResponseSink<>(
            this.remote.response(
                new RequestLine(RqMethod.GET, new CatalogUri(from, limit).string()),
                Headers.EMPTY,
                Content.EMPTY
            ),
            response -> {
                if (response.status() == RsStatus.OK) {
                    Catalog res = response::body;
                    return CompletableFuture.completedFuture(res);
                }
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unexpected status: " + response.status())
                );
            }
        ).result();
    }
}
