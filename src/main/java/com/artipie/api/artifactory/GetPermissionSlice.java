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

import com.artipie.RepoPermissions;
import com.artipie.Settings;
import com.artipie.api.RsJson;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.reactivestreams.Publisher;

/**
 * Artifactory `GET /api/security/permissions/{target}` endpoint, returns json with
 * permissions (= repository) information.
 * @since 0.10
 */
public final class GetPermissionSlice implements Slice {

    /**
     * Request line pattern to get username.
     */
    public static final Pattern PTRN = Pattern.compile("/api/security/permissions/(?<repo>[^/.]+)");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Artipie settings
     */
    public GetPermissionSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = GetPermissionSlice.PTRN.matcher(
            new RequestLineFrom(line).uri().toString()
        );
        final Response res;
        if (matcher.matches()) {
            final String repo = matcher.group("repo");
            res = new AsyncResponse(
                new RepoPermissions.FromSettings(this.settings).permissions(repo)
                    .thenApply(map -> new RsJson(GetPermissionSlice.response(map, repo)))
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Build json response.
     * @param permissions Users and permissions map
     * @param repo Repository name
     * @return Response JsonObject
     */
    private static JsonObject response(final Collection<RepoPermissions.UserPermission> permissions,
        final String repo) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final RepoPermissions.UserPermission perm : permissions) {
            final JsonArrayBuilder array = Json.createArrayBuilder();
            perm.permissions().forEach(array::add);
            builder.add(perm.username(), array.build());
        }
        return Json.createObjectBuilder()
            .add("repositories", Json.createArrayBuilder().add(repo).build())
            .add("principals", Json.createObjectBuilder().add("users", builder.build())).build();
    }
}
