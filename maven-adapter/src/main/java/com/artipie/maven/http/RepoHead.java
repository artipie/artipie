/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Head repository metadata.
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
        return this.client.response(
            new RequestLine(RqMethod.HEAD, path), Headers.EMPTY, Content.EMPTY
        ).thenApply(resp -> resp.status() == RsStatus.OK ? Optional.of(resp.headers()) : Optional.empty());
    }
}
