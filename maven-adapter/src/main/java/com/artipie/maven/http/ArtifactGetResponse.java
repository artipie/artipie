/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import com.artipie.maven.asto.RepositoryChecksums;

/**
 * Artifact {@code GET} response.
 * <p>
 * It includes a body of artifact requested if exists. The code is:
 * {@code 200} if exist and {@code 404} otherwise.
 * Also, it contains artifact headers if it exits.
 * @see ArtifactHeaders
 */
public final class ArtifactGetResponse extends Response.Wrap {

    /**
     * New artifact response.
     * @param storage Repository storage
     * @param location Artifact location
     */
    public ArtifactGetResponse(final Storage storage, final Key location) {
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
     * Ok {@code 200} response for {@code GET} request.
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
                    storage.value(location).thenCombine(
                        new RepositoryChecksums(storage).checksums(location),
                        (body, checksums) ->
                            new RsWithBody(
                                new RsWithHeaders(
                                    StandardRs.OK,
                                    ArtifactHeaders.from(location, checksums)
                                ),
                                body
                            )
                    )
                )
            );
        }
    }
}
