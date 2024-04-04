/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.misc.Pagination;
import com.artipie.http.Headers;
import com.artipie.http.RsStatus;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;

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
     * @param remote Remote repository.
     */
    public ProxyDocker(final Slice remote) {
        this.remote = remote;
    }

    @Override
    public Repo repo(String name) {
        return new ProxyRepo(this.remote, name);
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        return new ResponseSink<>(
            this.remote.response(
                new RequestLine(RqMethod.GET, pagination.uriWithPagination("/v2/_catalog")),
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
