/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromStorageFactory}.
 * @since 0.30
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AuthFromStorageFactoryTest {

    @Test
    void initsWhenStorageForAuthIsSet() throws IOException {
        MatcherAssert.assertThat(
            new AuthFromStorageFactory().getAuthentication(
                Yaml.createYamlInput(this.artipieEnvCreds()).readYamlMapping()
            ),
            new IsInstanceOf(AuthFromStorage.class)
        );
    }

    @Test
    void initsWhenPolicyIsSet() throws IOException {
        MatcherAssert.assertThat(
            new AuthFromStorageFactory().getAuthentication(
                Yaml.createYamlInput(this.artipieGithubCredsAndPolicy()).readYamlMapping()
            ),
            new IsInstanceOf(AuthFromStorage.class)
        );
    }

    private String artipieEnvCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: env",
            "  - type: artipie",
            "    storage:",
            "      type: fs",
            "      path: any"
        );
    }

    private String artipieGithubCredsAndPolicy() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: github",
            "  - type: artipie",
            "policy:",
            "  type: artipie",
            "  storage:",
            "    type: fs",
            "    path: /any/path"
        );
    }

}
