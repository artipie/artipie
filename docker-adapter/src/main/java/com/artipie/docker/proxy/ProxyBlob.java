/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
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
import io.reactivex.Flowable;

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
    private final Digest dig;

    /**
     * Blob size.
     */
    private final long bsize;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     * @param name Repository name.
     * @param dig Blob digest.
     * @param size Blob size.
     */
    public ProxyBlob(
        final Slice remote,
        final RepoName name,
        final Digest dig,
        final long size
    ) {
        this.remote = remote;
        this.name = name;
        this.dig = dig;
        this.bsize = size;
    }

    @Override
    public Digest digest() {
        return this.dig;
    }

    @Override
    public CompletionStage<Long> size() {
        return CompletableFuture.completedFuture(this.bsize);
    }

    @Override
    public CompletionStage<Content> content() {
        String blobPath = String.format("/v2/%s/blobs/%s", this.name.value(), this.dig.string());
        final CompletableFuture<Content> result = new CompletableFuture<>();
        this.remote.response(
            new RequestLine(RqMethod.GET, blobPath),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, headers, body) -> {
                final CompletableFuture<Void> sent;
                if (status == RsStatus.OK) {
                    final CompletableFuture<Void> terminated = new CompletableFuture<>();
                    result.complete(
                        new Content.From(
                            new ContentLength(headers).longValue(),
                            Flowable.fromPublisher(body)
                                .doOnError(terminated::completeExceptionally)
                                .doOnTerminate(() -> terminated.complete(null))
                        )
                    );
                    sent = terminated;
                } else {
                    sent = new FailedCompletionStage<Void>(
                        new ArtipieHttpException(
                            status,
                            String.format("Unexpected status: %s", status)
                        )
                    ).toCompletableFuture();
                }
                return sent;
            }
        ).handle(
            (nothing, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                }
                return nothing;
            }
        );
        return result;
    }
}
