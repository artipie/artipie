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
import com.artipie.asto.Key;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Artifactory `GET /api/security/permissions/{target}` endpoint, returns json with
 * permissions (= repository) information.
 * @since 0.10
 */
public final class GetPermissionSlice implements Slice {

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
        final Optional<String> opt = new FromRqLine(line, FromRqLine.RqPattern.REPO).get();
        return new AsyncResponse(
            opt.map(
                repo -> new Unchecked<>(this.settings::storage).value()
                    .exists(new Key.From(String.format("%s.yaml", repo))).thenCompose(
                        exists -> {
                            final CompletionStage<Response> res;
                            if (exists) {
                                res = new RepoPermissions.FromSettings(this.settings)
                                    .permissions(repo).thenApply(
                                        perms -> new RsJson(
                                            GetPermissionSlice.response(perms, repo)
                                        )
                                    );
                            } else {
                                res = CompletableFuture.completedStage(StandardRs.NOT_FOUND);
                            }
                            return res;
                        }
                    )
            ).orElse(CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST)))
        );
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
