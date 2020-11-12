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

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPermissions;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeletePermissionSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class DeletePermissionSliceTest {

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new DeletePermissionSlice(new InMemoryStorage()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/some/api/permissions/pypi")
            )
        );
    }

    @Test
    void returnsNotFoundIfRepositoryDoesNotExists() {
        MatcherAssert.assertThat(
            new DeletePermissionSlice(new InMemoryStorage()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/api/security/permissions/pypi")
            )
        );
    }

    @Test
    void deletesRepoPermissions() throws IOException {
        final Storage storage = new InMemoryStorage();
        final String repo = "docker";
        final Key.From key = new Key.From(String.format("%s.yaml", repo));
        new RepoConfigYaml(repo).withPermissions(
            new RepoPerms(
                List.of(
                    new RepoPermissions.PermissionItem("admin", "*"),
                    new RepoPermissions.PermissionItem("john", "delete"),
                    new RepoPermissions.PermissionItem("*", "download")
                )
            )
        ).saveTo(storage, repo);
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new DeletePermissionSlice(storage),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        String.format(
                            "Permission Target '%s' has been removed successfully.", repo
                        ).getBytes(StandardCharsets.UTF_8)
                    )
                ),
                new RequestLine(
                    RqMethod.DELETE, String.format("/api/security/permissions/%s", repo)
                )
            )
        );
        MatcherAssert.assertThat(
            "Removes permissions",
            Yaml.createYamlInput(
                new PublisherAs(storage.value(key).join())
                    .asciiString().toCompletableFuture().join()
            ).readYamlMapping().yamlMapping("repo").yamlMapping("permissions"),
            new IsNull<>()
        );
    }

}
