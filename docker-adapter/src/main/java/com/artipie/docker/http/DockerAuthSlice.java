/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.error.DeniedError;
import com.artipie.docker.error.UnauthorizedError;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Slice that wraps origin Slice replacing body with errors JSON in Docker API format
 * for 403 Unauthorized response status.
 */
final class DockerAuthSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * @param origin Origin slice.
     */
    DockerAuthSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine rqline, Headers rqheaders, Content rqbody) {
        return this.origin.response(rqline, rqheaders, rqbody)
            .thenApply(response -> {
                if (response.status() == RsStatus.UNAUTHORIZED) {
                    return ResponseBuilder.unauthorized()
                        .headers(response.headers())
                        .jsonBody(new UnauthorizedError().json())
                        .build();
                }
                if (response.status() == RsStatus.FORBIDDEN) {
                    return ResponseBuilder.forbidden()
                        .headers(response.headers())
                        .jsonBody(new DeniedError().json())
                        .build();
                }
                return response;
            });
    }
}
