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
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.RepoPermissions;
import com.artipie.RepoPerms;
import com.artipie.YamlPermissions;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.SliceAuth;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.cactoos.list.ListOf;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for basic authorisation and permissions settings,
 * {@link SliceAuth}, {@link YamlPermissions} and {@link AuthFromYaml} are involved.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class AuthAndPermissionsTest {

    @Test
    void allowsDownloadWhenAuthHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(StandardRs.EMPTY),
                new Permission.ByName("download", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("POST", "/bar", "HTTP/1.2").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void anyoneCanDownload() throws IOException {
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(StandardRs.EMPTY),
                new Permission.ByName("download", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("GET", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("anyone:000").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void anyoneCanNotDeploy() throws IOException {
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(StandardRs.EMPTY),
                new Permission.ByName("deploy", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("GET", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("anyone:000").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.UNAUTHORIZED)
        );
    }

    @Test
    void adminCanDownload() throws IOException {
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(StandardRs.EMPTY),
                new Permission.ByName("download", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("GET", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("admin:abc").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.OK)
        );
    }

    @Test
    void johnCanDelete() throws IOException {
        final RsStatus status = RsStatus.NO_CONTENT;
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(new RsWithStatus(status)),
                new Permission.ByName("delete", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("PUT", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("john:123").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(status)
        );
    }

    @Test
    void adminCanDeploy() throws IOException {
        final RsStatus status = RsStatus.ACCEPTED;
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(new RsWithStatus(status)),
                new Permission.ByName("deploy", this.permissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("PUT", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("admin:abc").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(status)
        );
    }

    @Test
    void authIsNotRequiredForPublicRepo() {
        final RsStatus status = RsStatus.ACCEPTED;
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(new RsWithStatus(status)),
                new Permission.ByName("install", this.allAllowedPermissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("GET", "/foo", "HTTP/1.2").toString(),
                Collections.emptyList(),
                Flowable.empty()
            ),
            new RsHasStatus(status)
        );
    }

    @Test
    void publicRepoWorksWithAuth() throws IOException {
        final RsStatus status = RsStatus.OK;
        MatcherAssert.assertThat(
            new SliceAuth(
                new SliceSimple(new RsWithStatus(status)),
                new Permission.ByName("delete", this.allAllowedPermissions()),
                new BasicIdentities(new AuthFromYaml(this.credentials()))
            ).response(
                new RequestLine("GET", "/foo", "HTTP/1.2").toString(),
                new ListOf<Map.Entry<String, String>>(
                    new Authorization(
                        String.format("Basic %s", new Base64Encoded("admin:abc").asString())
                    )
                ),
                Flowable.empty()
            ),
            new RsHasStatus(status)
        );
    }

    private YamlMapping credentials() {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(
                    "john",
                    Yaml.createYamlMappingBuilder().add("pass", "plain:123").build()
                )
                .add(
                    "admin",
                    Yaml.createYamlMappingBuilder().add("pass", "plain:abc").build()
                )
                .build()
        ).build();
    }

    private YamlPermissions permissions() {
        return new YamlPermissions(
            new RepoPerms(
                new ListOf<>(
                    new RepoPermissions.PermissionItem("admin", "*"),
                    new RepoPermissions.PermissionItem("*", "download"),
                    new RepoPermissions.PermissionItem("john", new ListOf<>("delete", "deploy"))
                )
            ).permsYaml()
        );
    }

    private YamlPermissions allAllowedPermissions() {
        return new YamlPermissions(new RepoPerms("*", "*").permsYaml());
    }

}
