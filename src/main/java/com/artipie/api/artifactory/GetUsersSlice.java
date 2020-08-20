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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * Artifactory `GET /api/security/users` endpoint, returns json with user names and links to
 * user information.
 * @since 0.10
 */
public final class GetUsersSlice implements Slice {

    /**
     * This endpoint path.
     */
    public static final String PATH = "/api/security/users";

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Setting
     */
    public GetUsersSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        Response res;
        try {
            final String base = this.settings.meta().string("base_url").replaceAll("/$", "");
            res = new AsyncResponse(
                this.settings.credentials().thenCompose(
                    opt -> opt.map(
                        cred -> cred.users().<Response>thenApply(
                            list -> {
                                final JsonArrayBuilder json = Json.createArrayBuilder();
                                list.forEach(
                                    user -> json.add(GetUsersSlice.getUserJson(user, base))
                                );
                                return new RsJson(json);
                            }
                        )
                    ).orElse(CompletableFuture.completedFuture(StandardRs.NOT_FOUND))
                )
            );
        } catch (final IOException err) {
            Logger.error(this, err.getMessage());
            res = new RsWithStatus(RsStatus.INTERNAL_ERROR);
        }
        return res;
    }

    /**
     * Returns json for user.
     * @param name Username
     * @param base Base url
     * @return User json object
     */
    private static JsonObject getUserJson(final String name, final String base) {
        return Json.createObjectBuilder()
            .add("name", name)
            .add("uri", String.format("%s/api/security/users/%s", base, name))
            .add("realm", "Internal")
            .build();
    }
}
