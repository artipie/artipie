/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.error.DeniedError;
import com.artipie.docker.error.UnauthorizedError;
import com.artipie.http.BaseResponse;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;

/**
 * Slice that wraps origin Slice replacing body with errors JSON in Docker API format
 * for 403 Unauthorized response status.
 *
 * @since 0.5
 */
final class DockerAuthSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     */
    DockerAuthSlice(final Slice origin) {
        this.origin = origin;
    }

    @Override
    public Response response(
        final RequestLine rqline,
        final Headers rqheaders,
        final Content rqbody) {
        final Response response = this.origin.response(rqline, rqheaders, rqbody);
        return connection -> response.send(
            (rsstatus, rsheaders, rsbody) -> {
                if (rsstatus == RsStatus.UNAUTHORIZED) {
                    return BaseResponse.unauthorized()
                        .headers(rsheaders)
                        .jsonBody(new UnauthorizedError().json())
                        .send(connection);
                }
                if (rsstatus == RsStatus.FORBIDDEN) {
                    return BaseResponse.forbidden()
                        .headers(rsheaders)
                        .jsonBody(new DeniedError().json())
                        .send(connection);
                }
                return connection.accept(rsstatus, rsheaders, rsbody);
            }
        );
    }
}
