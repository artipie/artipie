/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Responses on api key requests.
 *
 * @since 0.3
 */
final class ApiKeySlice implements Slice {

    /**
     * The users.
     */
    private final Authentication auth;

    /**
     * The Ctor.
     * @param auth Auth.
     */
    ApiKeySlice(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth)
                .authenticate(headers)
                .thenApply(AuthScheme.Result::user)
                .thenApply(
                    opt -> opt.flatMap(
                        user -> new RqHeaders(headers, Authorization.NAME).stream()
                            .filter(val -> val.startsWith(BasicAuthScheme.NAME))
                            .map(val -> val.substring(BasicAuthScheme.NAME.length() + 1))
                            .findFirst()
                    )
                ).thenApply(
                    key -> key.<Response>map(val -> new RsWithBody(val, StandardCharsets.US_ASCII))
                        .orElseGet(() -> new RsWithStatus(RsStatus.UNAUTHORIZED))
                )
        );
    }
}
