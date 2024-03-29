/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rq.RequestLine;

import javax.json.Json;
import javax.json.JsonStructure;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;

/**
 * Slice to handle `GET /user` request.
 */
final class GetUserSlice implements Slice {

    /**
     * Authentication.
     */
    private final AuthScheme scheme;

    /**
     * Ctor.
     * @param scheme Authentication
     */
    GetUserSlice(final AuthScheme scheme) {
        this.scheme = scheme;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(final RequestLine line, final Headers headers,
                                                    final Content body) {
        return this.scheme.authenticate(headers, line)
            .toCompletableFuture()
            .thenApply(
                result -> {
                    if (result.status() != AuthScheme.AuthStatus.FAILED) {
                        return ResponseBuilder.ok()
                            .jsonBody(GetUserSlice.json(result.user().name()))
                            .build();
                    }
                    return ResponseBuilder.unauthorized()
                        .header(new WwwAuthenticate(result.challenge()))
                        .build();
                }
            );
    }

    /**
     * Json response with user info.
     * @param name Username
     * @return User info as JsonStructure
     */
    private static JsonStructure json(final String name) {
        return Json.createReader(
            new StringReader(
                String.join(
                    "\n",
                    "{",
                    "  \"company\": \"Artipie\",",
                    "  \"created_at\": \"2020-08-01 13:06:29.212000+00:00\",",
                    "  \"description\": \"\",",
                    "  \"location\": \"\",",
                    String.format("  \"login\": \"%s\",", name),
                    String.format("  \"name\": \"%s\",", name),
                    "  \"url\": \"\",",
                    "  \"user_type\": \"user\"",
                    "}"
                )
            )
        ).read();
    }
}
