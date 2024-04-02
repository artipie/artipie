/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.BlobSource;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Proxy implementation of {@link Layers}.
 *
 * @since 0.3
 */
public final class ProxyLayers implements Layers {

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     * @param name Repository name.
     */
    public ProxyLayers(final Slice remote, final RepoName name) {
        this.remote = remote;
        this.name = name;
    }

    @Override
    public CompletableFuture<Blob> put(final BlobSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Blob> mount(final Blob blob) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Optional<Blob>> get(final Digest digest) {
        String blobPath = String.format("/v2/%s/blobs/%s", this.name.value(), digest.string());
        return new ResponseSink<>(
            this.remote.response(new RequestLine(RqMethod.HEAD, blobPath), Headers.EMPTY, Content.EMPTY),
            response -> {
                final CompletableFuture<Optional<Blob>> result;
                if (response.status() == RsStatus.OK) {
                    result = CompletableFuture.completedFuture(
                        Optional.of(
                            new ProxyBlob(
                                this.remote,
                                this.name,
                                digest,
                                new ContentLength(response.headers()).longValue()
                            )
                        )
                    );
                } else if (response.status() == RsStatus.NOT_FOUND) {
                    result = CompletableFuture.completedFuture(Optional.empty());
                } else {
                    result = CompletableFuture.failedFuture(
                        new IllegalArgumentException("Unexpected status: " + response.status())
                    );
                }
                return result;
            }
        ).result();
    }
}
