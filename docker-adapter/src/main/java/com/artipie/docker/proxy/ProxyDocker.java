/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return new ResponseSink<>(
            this.remote.response(
                new RequestLine(RqMethod.GET, new CatalogUri(from, limit).string()).toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            (status, headers, body) -> {
                final CompletionStage<Catalog> result;
                if (status == RsStatus.OK) {
                    return CompletableFuture.completedFuture(() -> new Content.From(body));
                } else {
                    result = new FailedCompletionStage<>(
                        new IllegalArgumentException("Unexpected status: " + status)
                    );
                }
                return result;
            }
        ).result();
    }
}
