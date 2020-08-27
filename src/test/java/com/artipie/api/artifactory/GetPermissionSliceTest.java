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
import com.artipie.UtilRepoPermissions;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GetPermissionSlice}.
 * @since 0.10
 * @todo #495:30min Create class to generate repository permissions settings
 *  Methods addSettings() and addEmpty() are copied from RepoPermissionsFromSettingsTest, let't
 *  extract them into class to avoid code duplication. Class can be introduced in test scope. Also,
 *  let's check other tests for similar functionality and replace all duplications.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GetPermissionSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new GetPermissionSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/some/api/permissions/maven")
            )
        );
    }

    @Test
    void returnsEmptyUsersIfNoPermissionsSet() {
        final String repo = "docker";
        final UtilRepoPermissions perm = new UtilRepoPermissions(this.storage);
        perm.addEmpty(repo);
        MatcherAssert.assertThat(
            new GetPermissionSlice(new Settings.Fake(this.storage)),
            new SliceHasResponse(
                new RsHasBody(this.response(repo, Json.createObjectBuilder().build())),
                new RequestLine(RqMethod.GET, String.format("/api/security/permissions/%s", repo))
            )
        );
    }

    @Test
    void returnsUsersAndPermissionsList() {
        final String repo = "maven";
        final String john = "john";
        final String mark = "mark";
        final String download = "download";
        final String upload = "upload";
        final UtilRepoPermissions perm = new UtilRepoPermissions(this.storage);
        perm.addSettings(
            repo,
            new MapOf<String, List<String>>(
                new MapEntry<>(john, new ListOf<String>(download, upload)),
                new MapEntry<>(mark, new ListOf<String>(download))
            )
        );
        MatcherAssert.assertThat(
            new GetPermissionSlice(new Settings.Fake(this.storage)),
            new SliceHasResponse(
                new RsHasBody(
                    this.response(
                        repo,
                        Json.createObjectBuilder()
                            .add(john, Json.createArrayBuilder().add(download).add(upload).build())
                            .add(mark, Json.createArrayBuilder().add(download).build())
                            .build()
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/api/security/permissions/%s", repo))
            )
        );
    }

    private byte[] response(final String repo, final JsonObject users) {
        return Json.createObjectBuilder()
            .add("repositories", Json.createArrayBuilder().add(repo).build())
            .add(
                "principals",
                Json.createObjectBuilder().add("users", users)
            ).build().toString().getBytes(StandardCharsets.UTF_8);
    }
}
