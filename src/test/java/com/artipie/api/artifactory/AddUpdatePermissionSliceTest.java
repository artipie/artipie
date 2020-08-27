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

import com.artipie.BuildingRepoPermissions;
import com.artipie.Settings;
import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AddUpdatePermissionSlice}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AddUpdatePermissionSliceTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsBadRequestOnInvalidRequest() {
        MatcherAssert.assertThat(
            new AddUpdatePermissionSlice(new Settings.Fake()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/some/api/permissions/maven")
            )
        );
    }

    @Test
    void updatesPermissions() throws IOException {
        final String repo = "maven";
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addEmpty(repo);
        MatcherAssert.assertThat(
            "Returns 200 OK",
            new AddUpdatePermissionSlice(new Settings.Fake(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.PUT, String.format("/api/security/permissions/%s", repo)),
                Headers.EMPTY,
                new Content.From(this.json().getBytes(StandardCharsets.UTF_8))
            )
        );
        MatcherAssert.assertThat(
            "Sets permissions for bob",
            perm.permissionsForUser(repo, "bob"),
            Matchers.containsInAnyOrder("read", "write", "manage")
        );
        MatcherAssert.assertThat(
            "Sets permissions for alice",
            perm.permissionsForUser(repo, "alice"),
            Matchers.containsInAnyOrder("write", "annotate", "read")
        );
    }

    private String json() {
        return String.join(
            "\n",
            "{",
            " \"name\": \"java-developers\",",
            " \"repo\": {",
            "    \"include-patterns\": [\"**\"],",
            "    \"exclude-patterns\": [\"\"],",
            "    \"repositories\": [\"local-rep1\", \"remote-rep1\", \"virtual-rep2\"],",
            "    \"actions\": {",
            "          \"users\" : {",
            "            \"bob\": [\"read\",\"write\",\"manage\"],",
            "            \"alice\" : [\"write\",\"annotate\", \"read\"]",
            "          },",
            "          \"groups\" : {",
            "            \"dev-leads\" : [\"manage\",\"read\",\"annotate\"],",
            "            \"readers\" : [\"read\"]",
            "          }",
            "    }",
            "  },",
            "\"build\": {",
            "    \"include-patterns\": [\"\"],",
            "    \"exclude-patterns\": [\"\"],",
            "    \"repositories\": [\"artifactory-build-info\"],",
            "    \"actions\": {",
            "          \"users\" : {",
            "            \"bob\": [\"read\",\"manage\"],",
            "            \"alice\" : [\"write\"]",
            "          },",
            "          \"groups\" : {",
            "            \"dev-leads\" : [\"manage\",\"read\",\"write\",\"annotate\",\"delete\"],",
            "            \"readers\" : [\"read\"]",
            "          }",
            "    }",
            "  }",
            "}"
        );
    }

}
