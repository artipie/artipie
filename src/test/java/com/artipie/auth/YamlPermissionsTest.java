/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.AuthUser;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link YamlPermissions}.
 * @since 0.2
 * @checkstyle LeftCurlyCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlPermissionsTest {

    @Test
    void johnCanDownloadDeployDrinkAndDelete() throws Exception {
        final AuthUser user = new AuthUser("john", "test");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "drink"); }),
                    new MatcherOf<>(perm -> !perm.allowed(user, "install"))
                )
            )
        );
    }

    @Test
    void janeCanDownloadDrinkAndDeploy() throws Exception {
        final AuthUser user = new AuthUser("jane", "test");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "drink"); }),
                    new MatcherOf<>(perm -> !perm.allowed(user, "install")),
                    new MatcherOf<>(perm -> !perm.allowed(user, "update"))
                )
            )
        );
    }

    @Test
    void annCanDoAnything() throws Exception {
        final AuthUser user = new AuthUser("ann", "test");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "drink"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "any_action"); })
                )
            )
        );
    }

    @Test
    void anyoneCanDownloadAndDrink() throws Exception {
        MatcherAssert.assertThat(
            this.permissions().allowed(new AuthUser("anyone", "test"), "download"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.permissions().allowed(new AuthUser("*", "test"), "download"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.permissions().allowed(new AuthUser("*", "test"), "drink"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            this.permissions().allowed(new AuthUser("Someone", "test"), "drink"),
            new IsEqual<>(true)
        );
    }

    @Test
    void adminCanDoAnything() throws Exception {
        final AuthUser user = new AuthUser("admin", "test");
        MatcherAssert.assertThat(
            this.permissions(),
            new AllOf<YamlPermissions>(
                new ListOf<org.hamcrest.Matcher<? super YamlPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(user, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(user, "install"); })
                )
            )
        );
    }

    /**
     * Permissions from repo-full-config.yml example file.
     *
     * @return Permissions parsed from file.
     */
    private YamlPermissions permissions() throws IOException {
        return new YamlPermissions(
            Yaml.createYamlInput(new TestResource("repo-full-config.yml").asPath().toFile())
                .readYamlMapping()
                .yamlMapping("repo")
                .yamlMapping("permissions")
        );
    }

}
