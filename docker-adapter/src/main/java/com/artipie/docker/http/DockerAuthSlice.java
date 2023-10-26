/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.error.DeniedError;
import com.artipie.docker.error.UnauthorizedError;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

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
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final Publisher<ByteBuffer> rqbody) {
        final Response response = this.origin.response(rqline, rqheaders, rqbody);
        return connection -> response.send(
            (rsstatus, rsheaders, rsbody) -> {
                final CompletionStage<Void> sent;
                if (rsstatus == RsStatus.UNAUTHORIZED) {
                    sent = new RsWithHeaders(
                        new ErrorsResponse(rsstatus, new UnauthorizedError()),
                        rsheaders
                    ).send(connection);
                } else if (rsstatus == RsStatus.FORBIDDEN) {
                    sent = new RsWithHeaders(
                        new ErrorsResponse(rsstatus, new DeniedError()),
                        rsheaders
                    ).send(connection);
                } else {
                    sent = connection.accept(rsstatus, rsheaders, rsbody);
                }
                return sent;
            }
        );
    }
}
