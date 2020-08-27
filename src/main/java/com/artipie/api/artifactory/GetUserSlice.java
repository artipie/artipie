/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.api.artifactory;

import com.artipie.Settings;
import com.artipie.api.RsJson;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Artifactory `GET /api/security/users/{userName}` endpoint, returns user information.
 * @since 0.10
 */
public final class GetUserSlice implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Artipie setting
     */
    public GetUserSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> user = new FromRqLine(line, FromRqLine.RqPattern.USER).get();
        return user.<Response>map(
            username -> new AsyncResponse(
                this.settings.credentials().thenCompose(
                    cred ->  cred.users().thenApply(
                        users -> users.contains(username)
                    ).thenApply(
                        has -> {
                            final Response resp;
                            if (has) {
                                resp = new RsJson(
                                    () -> Json.createObjectBuilder()
                                        .add("name", username)
                                        .add("email", String.format("%s@artipie.com", username))
                                        .add("lastLoggedIn", "2020-01-01T01:01:01.000+01:00")
                                        .add("realm", "Internal")
                                        .build(),
                                    StandardCharsets.UTF_8
                                );
                            } else {
                                resp = StandardRs.NOT_FOUND;
                            }
                            return resp;
                        }
                    )
                )
            )
        ).orElse(StandardRs.NOT_FOUND);
    }
}
