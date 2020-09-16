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
import com.artipie.asto.ext.PublisherAs;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.reactivestreams.Publisher;

/**
 * Artifactory `PUT /api/security/permissions/{target}` endpoint, updates `permissions` section
 * in repository section.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AddUpdatePermissionSlice implements Slice {

    /**
     * Permissions mapping: translates artifactory permissions on Artipie language.
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private static final Map<String, String> MAPPING =
        new MapOf<>(
            new MapEntry<>("r", "read"),
            new MapEntry<>("read", "read"),
            new MapEntry<>("w", "write"),
            new MapEntry<>("write", "write"),
            new MapEntry<>("deploy", "write"),
            new MapEntry<>("m", "*"),
            new MapEntry<>("admin", "*"),
            new MapEntry<>("manage", "*"),
            new MapEntry<>("d", "delete"),
            new MapEntry<>("delete", "delete")
        );

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Ctor.
     * @param settings Setting
     */
    public AddUpdatePermissionSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Optional<String> opt = new FromRqLine(line, FromRqLine.RqPattern.REPO).get();
        return opt.<Response>map(
            repo -> new AsyncResponse(
                new PublisherAs(body).bytes().thenApply(
                    bytes -> Json.createReader(new ByteArrayInputStream(bytes)).readObject()
                ).thenApply(
                    AddUpdatePermissionSlice::permissions
                ).thenCompose(
                    perms -> new RepoPermissions.FromSettings(this.settings)
                        .addUpdate(repo, perms)
                ).thenApply(
                    ignored -> StandardRs.EMPTY
                )
            )
        ).orElse(new RsWithStatus(RsStatus.BAD_REQUEST));
    }

    /**
     * Converts json permissions into list of {@link RepoPermissions.UserPermission}.
     * @param json Json body
     * @return List of {@link RepoPermissions.UserPermission}
     */
    private static List<RepoPermissions.UserPermission> permissions(final JsonObject json) {
        final JsonObject users = json.getJsonObject("repo").getJsonObject("actions")
            .getJsonObject("users");
        final List<RepoPermissions.UserPermission> res = new ArrayList<>(users.size());
        users.forEach(
            (user, perms) ->
                res.add(
                    new RepoPermissions.UserPermission(
                        user, perms.asJsonArray().stream()
                        .map(item -> item.toString().replace("\"", ""))
                        .map(
                            item -> Optional.ofNullable(
                                AddUpdatePermissionSlice.MAPPING.get(item)
                            ).orElseThrow(
                                () -> new IllegalArgumentException(
                                    String.format("Unsupported permission '%s'!", item)
                                )
                            )
                        ).distinct().collect(Collectors.toList())
                    )
                )
        );
        return res;
    }
}
