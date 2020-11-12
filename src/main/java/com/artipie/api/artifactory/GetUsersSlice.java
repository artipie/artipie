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

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.Users;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Map;
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
     * Artipie users.
     */
    private final Users users;

    /**
     * Artipie meta config.
     */
    private final YamlMapping meta;

    /**
     * Ctor.
     * @param users Users
     * @param meta Meta info
     */
    public GetUsersSlice(final Users users, final YamlMapping meta) {
        this.users = users;
        this.meta = meta;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String base = this.meta.string("base_url").replaceAll("/$", "");
        return new AsyncResponse(
            this.users.list().<Response>thenApply(
                list -> {
                    final JsonArrayBuilder json = Json.createArrayBuilder();
                    list.forEach(user -> json.add(GetUsersSlice.getUserJson(base, user.name())));
                    return new RsJson(json);
                }
            )
        );
    }

    /**
     * Returns json for user.
     * @param base Base url
     * @param name Username
     * @return User json object
     */
    private static JsonObject getUserJson(final String base, final String name) {
        return Json.createObjectBuilder()
            .add("name", name)
            .add("uri", String.format("%s/api/security/users/%s", base, name))
            .add("realm", "Internal")
            .build();
    }
}
