/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonStructure;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `GET /user` request.
 * @since 0.4
 * @todo #32:30min Implement this slice properly, check swagger docs for more details
 *  https://api.anaconda.org/docs#!/user/get_user
 *  Now it returns string stab.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.scheme.authenticate(headers, line).thenApply(
                result -> {
                    final Response res;
                    if (result.user().isPresent()) {
                        res = new RsJson(
                            () -> GetUserSlice.json(result.user().get().name()),
                            StandardCharsets.UTF_8
                        );
                    } else {
                        res = new RsWithHeaders(
                            new RsWithStatus(RsStatus.UNAUTHORIZED),
                            new Headers.From(new WwwAuthenticate(result.challenge()))
                        );
                    }
                    return res;
                }
            )
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
