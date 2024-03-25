/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.ResponseBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Responses on api key requests.
 */
final class ApiKeySlice implements Slice {

    /**
     * The users.
     */
    private final Authentication auth;

    /**
     * @param auth Auth.
     */
    ApiKeySlice(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth)
                .authenticate(headers)
                .thenApply(
                    result -> {
                        if (result.status() == AuthScheme.AuthStatus.AUTHENTICATED) {
                            final Optional<String> key = new RqHeaders(headers, Authorization.NAME)
                                .stream()
                                .filter(val -> val.startsWith(BasicAuthScheme.NAME))
                                .map(val -> val.substring(BasicAuthScheme.NAME.length() + 1))
                                .findFirst();
                            if (key.isPresent()) {
                                return ResponseBuilder.ok()
                                    .textBody(key.get(), StandardCharsets.US_ASCII)
                                    .build();
                            }
                        }
                        return ResponseBuilder.unauthorized().build();
                    }
                )
        );
    }
}
