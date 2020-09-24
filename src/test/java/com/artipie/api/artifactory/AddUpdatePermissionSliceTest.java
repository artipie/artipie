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
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.RepoPerms;
import com.artipie.Settings;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
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
    void updatesPermissionsAndPatterns() throws IOException {
        final String repo = "maven";
        final RepoPerms perm = new RepoPerms();
        perm.saveSettings(this.storage, repo);
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
            this.permissionsFor(repo, "bob"),
            Matchers.containsInAnyOrder("read", "write", "*")
        );
        MatcherAssert.assertThat(
            "Sets permissions for alice",
            this.permissionsFor(repo, "alice"),
            Matchers.containsInAnyOrder("write", "read")
        );
        MatcherAssert.assertThat(
            "Sets permissions for john",
            this.permissionsFor(repo, "john"),
            Matchers.containsInAnyOrder("*")
        );
        MatcherAssert.assertThat(
            "Sets patterns",
            this.patterns(repo),
            Matchers.contains("**", "maven/**")
        );
        MatcherAssert.assertThat(
            "Sets readers group",
            this.permissionsFor(repo, "/readers"),
            Matchers.contains("read")
        );
    }

    @Test
    void validatesPatterns() {
        final String repo = "docker";
        final RepoPerms perm = new RepoPerms();
        perm.saveSettings(this.storage, repo);
        MatcherAssert.assertThat(
            new AddUpdatePermissionSlice(new Settings.Fake(this.storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, String.format("/api/security/permissions/%s", repo)),
                Headers.EMPTY,
                new Content.From(
                    String.join(
                        "\n",
                        "{",
                        " \"repo\": {",
                        "    \"include-patterns\": [\"some/path/**\u002F*.txt\"],",
                        "    \"repositories\": [\"local-repo\"],",
                        "    \"actions\": { \"users\" : { \"john\" : [\"admin\"] } }",
                        "  }",
                        "}"
                    ).getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }

    private List<String> permissionsFor(final String repo, final String user)
        throws IOException {
        return this.repo(repo).yamlMapping("permissions").yamlSequence(user)
            .values().stream().map(node -> node.asScalar().value())
            .collect(Collectors.toList());
    }

    private Collection<String> patterns(final String repo)
        throws IOException {
        return this.repo(repo)
            .yamlSequence("permissions_include_patterns")
            .values().stream().map(node -> node.asScalar().value())
            .collect(Collectors.toList());
    }

    private YamlMapping repo(final String repo) throws IOException {
        return Yaml.createYamlInput(
            new PublisherAs(this.storage.value(new Key.From(String.format("%s.yaml", repo))).join())
                .asciiString().toCompletableFuture().join()
        ).readYamlMapping().yamlMapping("repo");
    }

    private String json() {
        return String.join(
            "\n",
            "{",
            " \"name\": \"java-developers\",",
            " \"repo\": {",
            "    \"include-patterns\": [\"**\", \"maven/**\"],",
            "    \"exclude-patterns\": [\"\"],",
            "    \"repositories\": [\"local-rep1\", \"remote-rep1\", \"virtual-rep2\"],",
            "    \"actions\": {",
            "          \"users\" : {",
            "            \"bob\": [\"r\",\"write\",\"manage\"],",
            "            \"alice\" : [\"w\", \"read\"],",
            "            \"john\" : [\"admin\"]",
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
