/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.jcabi.log.Logger;
import io.reactivex.Flowable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Head repository metadata.
 * @since 0.5
 */
final class RepoHead {

    /**
     * Client slice.
     */
    private final Slice client;

    /**
     * New repository artifact's heads.
     * @param client Client slice
     */
    RepoHead(final Slice client) {
        this.client = client;
    }

    /**
     * Artifact head.
     * @param path Path for artifact
     * @return Artifact headers
     */
    CompletionStage<Optional<Headers>> head(final String path) {
        final CompletableFuture<Optional<Headers>> promise = new CompletableFuture<>();
        return this.client.response(
            new RequestLine(RqMethod.HEAD, path).toString(), Headers.EMPTY, Flowable.empty()
        ).send(
            (status, rsheaders, body) -> {
                final CompletionStage<Optional<Headers>> res;
                if (status == RsStatus.OK) {
                    res = CompletableFuture.completedFuture(Optional.of(rsheaders));
                } else {
                    res = CompletableFuture.completedFuture(Optional.empty());
                }
                return res.thenAccept(promise::complete).toCompletableFuture();
            }
        ).handle(
            (nothing, throwable) -> {
                if (throwable != null) {
                    Logger.error(this, throwable.getMessage());
                    promise.completeExceptionally(throwable);
                }
                return null;
            }
        ).thenCompose(nothing -> promise);
    }
}
