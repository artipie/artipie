/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import com.artipie.maven.asto.RepositoryChecksums;

/**
 * Artifact {@code HEAD} response.
 * <p>
 * It doesn't include a body, only status code for artifact: {@code 200} if exist and {@code 404}
 * otherwise. Also, it contains artifact headers if it exits.
 * </p>
 * @see ArtifactHeaders
 * @since 0.5
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
                        final Response rsp;
                        if (exists) {
                            rsp = new OkResponse(storage, location);
                        } else {
                            rsp = StandardRs.NOT_FOUND;
                        }
                        return rsp;
                    }
                )
            )
        );
    }

    /**
     * Ok {@code 200} response for {@code HEAD} request.
     * @since 0.5
     */
    private static final class OkResponse extends Response.Wrap {

        /**
         * New response.
         * @param storage Repository storage
         * @param location Artifact location
         */
        OkResponse(final Storage storage, final Key location) {
            super(
                new AsyncResponse(
                    new RepositoryChecksums(storage).checksums(location).thenApply(
                        checksums -> new RsWithHeaders(
                            StandardRs.OK, new ArtifactHeaders(location, checksums)
                        )
                    )
                )
            );
        }
    }
}
