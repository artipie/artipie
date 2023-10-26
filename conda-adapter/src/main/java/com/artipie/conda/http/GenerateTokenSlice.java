/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice for token authorization.
 * @since 0.4
 */
final class GenerateTokenSlice implements Slice {

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Tokens.
     */
    private final Tokens tokens;

    /**
     * Ctor.
     * @param auth Authentication
     * @param tokens Tokens
     */
    GenerateTokenSlice(final Authentication auth, final Tokens tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth).authenticate(headers).thenApply(
                usr -> {
                    final Response res;
                    if (usr.user().isPresent()) {
                        res = new RsJson(
                            () -> Json.createObjectBuilder()
                                .add("token", this.tokens.generate(usr.user().get())).build(),
                            StandardCharsets.UTF_8
                        );
                    } else {
                        res = new RsWithHeaders(
                            new RsWithStatus(RsStatus.UNAUTHORIZED),
                            new Headers.From(new WwwAuthenticate(usr.challenge()))
                        );
                    }
                    return res;
                }
            )
        );
    }
}
