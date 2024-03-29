/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Proxy implementation of {@link Blob}.
 */
public final class ProxyBlob implements Blob {

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Blob digest.
     */
    private final Digest digest;

    /**
     * Blob size.
     */
    private final long blobSize;

    /**
     * @param remote Remote repository.
     * @param name Repository name.
     * @param digest Blob digest.
     * @param size Blob size.
     */
    public ProxyBlob(Slice remote, RepoName name, Digest digest, long size
    ) {
        this.remote = remote;
        this.name = name;
        this.digest = digest;
        this.blobSize = size;
    }

    @Override
    public Digest digest() {
        return this.digest;
    }

    @Override
    public CompletionStage<Long> size() {
        return CompletableFuture.completedFuture(this.blobSize);
    }

    @Override
    public CompletionStage<Content> content() {
        String blobPath = String.format("/v2/%s/blobs/%s", this.name.value(), this.digest.string());
        return this.remote
            .response(new RequestLine(RqMethod.GET, blobPath), Headers.EMPTY, Content.EMPTY)
            .thenCompose(response -> {
                if (response.status() == RsStatus.OK) {
                    Content res = response.headers().find(ContentLength.NAME)
                        .stream().findFirst()
                        .map(h -> Long.parseLong(h.getValue()))
                        .map(val -> (Content) new Content.From(val, response.body()))
                        .orElseGet(response::body);
                    return CompletableFuture.completedFuture(res);
                }
                return CompletableFuture.failedFuture(
                    new ArtipieHttpException(response.status(), "Unexpected status: " + response.status())
                );
            });
    }
}
