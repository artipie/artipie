/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.BaseResponse;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.maven.asto.RepositoryChecksums;

/**
 * Artifact {@code HEAD} response.
 * <p>
 * It doesn't include a body, only status code for artifact: {@code 200} if exist and {@code 404}
 * otherwise. Also, it contains artifact headers if it exits.
 * </p>
 * @see ArtifactHeaders
 */
public final class ArtifactHeadResponse extends Response.Wrap {

    /**
     * New artifact response.
     * @param storage Repository storage
     * @param location Artifact location
     */
    public ArtifactHeadResponse(final Storage storage, final Key location) {
        super(
            new AsyncResponse(
                storage.exists(location).thenApply(
                    exists -> {
                        if (exists) {
                            return new AsyncResponse(
                                new RepositoryChecksums(storage)
                                    .checksums(location)
                                    .thenApply(
                                        checksums -> BaseResponse.ok()
                                            .headers(ArtifactHeaders.from(location, checksums))
                                    )
                            );
                        }
                        return BaseResponse.notFound();
                    }
                )
            )
        );
    }
}
