/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.CredsConfigYaml;
import com.artipie.YamlPermissions;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.repo.RepoPermissions;
import com.artipie.repo.RepoPerms;
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
 * {@link BasicAuthSlice}, {@link YamlPermissions} and {@link AuthFromYaml} are involved.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class AuthAndPermissionsTest {

    @Test
    void allowsDownloadWhenAuthHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            new BasicAuthSlice(
                new SliceSimple(StandardRs.EMPTY),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("download", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(StandardRs.EMPTY),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("download", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(StandardRs.EMPTY),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("deploy", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(StandardRs.EMPTY),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("download", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(new RsWithStatus(status)),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("delete", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(new RsWithStatus(status)),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("deploy", this.permissions())
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
            new BasicAuthSlice(
                new SliceSimple(new RsWithStatus(status)),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("install", this.allAllowedPermissions())
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
            new BasicAuthSlice(
                new SliceSimple(new RsWithStatus(status)),
                new AuthFromYaml(this.credentials()),
                new Permission.ByName("delete", this.allAllowedPermissions())
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
        return new CredsConfigYaml().withUserAndPlainPswd("john", "123")
            .withUserAndPlainPswd("admin", "abc").yaml();
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
