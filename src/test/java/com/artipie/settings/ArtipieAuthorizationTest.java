/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.security.policy.CachedYamlPolicy;
import com.artipie.security.policy.Policy;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ArtipieAuthorization.FromYaml}.
 * @since 0.29
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class ArtipieAuthorizationTest {

    @Test
    void initializesEnvAuth() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.envCreds()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Env credentials are initialized",
            authz.authentication().toString(),
            new StringContains("AuthFromEnv")
        );
        MatcherAssert.assertThat(
            "Policy is free",
            authz.policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
        MatcherAssert.assertThat(
            "Policy storage is absent",
            authz.policyStorage().isEmpty()
        );
    }

    @Test
    void initializesGithubAuth() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.githubCreds()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Github auth created",
            authz.authentication().toString(),
            new StringContains("GithubAuth")
        );
        MatcherAssert.assertThat(
            "Policy is free",
            authz.policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
        MatcherAssert.assertThat(
            "Policy storage is absent",
            authz.policyStorage().isEmpty()
        );
    }

    @Test
    void initializesKeycloakAuth() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.keycloakCreds()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Keycloak storage created",
            authz.authentication().toString(),
            new StringContains("AuthFromKeycloak")
        );
        MatcherAssert.assertThat(
            "Policy is free",
            authz.policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
        MatcherAssert.assertThat(
            "Policy storage is absent",
            authz.policyStorage().isEmpty()
        );
    }

    @Test
    void initializesArtipieAuth() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.artipieCreds()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Auth from storage initiated",
            authz.authentication().toString(),
            new StringContains("AuthFromStorage")
        );
        MatcherAssert.assertThat(
            "Policy is free",
            authz.policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
        MatcherAssert.assertThat(
            "Policy storage is present",
            authz.policyStorage().isPresent()
        );
    }

    @Test
    void initializesArtipieAuthAndPolicy() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.artipieCredsWithPolicy()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Auth from storage initiated",
            authz.authentication().toString(),
            new StringContains("AuthFromStorage")
        );
        MatcherAssert.assertThat(
            "CachedYamlPolicy created",
            authz.policy(),
            new IsInstanceOf(CachedYamlPolicy.class)
        );
        MatcherAssert.assertThat(
            "Policy storage is present",
            authz.policyStorage().isPresent()
        );
    }

    @Test
    void initializesAllAuths() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.artipieGithubKeycloakEnvCreds()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Auth from storage, github, env and keycloak initiated",
            authz.authentication().toString(),
            Matchers.allOf(
                new StringContains("AuthFromStorage"),
                new StringContains("AuthFromKeycloak"),
                new StringContains("GithubAuth"),
                new StringContains("AuthFromEnv")
            )
        );
        MatcherAssert.assertThat(
            "Empty policy created",
            authz.policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
        MatcherAssert.assertThat(
            "Policy storage is present",
            authz.policyStorage().isPresent()
        );
    }

    @Test
    void initializesAllAuthsAndPolicy() throws IOException {
        final ArtipieAuthorization authz = new ArtipieAuthorization.FromYaml(
            Yaml.createYamlInput(this.artipieGithubKeycloakEnvCredsAndPolicy()).readYamlMapping()
        );
        MatcherAssert.assertThat(
            "Auth from storage, github, env and keycloak initiated",
            authz.authentication().toString(),
            Matchers.allOf(
                new StringContains("AuthFromStorage"),
                new StringContains("AuthFromKeycloak"),
                new StringContains("GithubAuth"),
                new StringContains("AuthFromEnv")
            )
        );
        MatcherAssert.assertThat(
            "CachedYamlPolicy created",
            authz.policy(),
            new IsInstanceOf(CachedYamlPolicy.class)
        );
        MatcherAssert.assertThat(
            "Policy storage is present",
            authz.policyStorage().isPresent()
        );
    }

    private String envCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: env"
        );
    }

    private String githubCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: github"
        );
    }

    private String keycloakCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: keycloak",
            "    url: http://any",
            "    realm: any",
            "    client-id: any",
            "    client-password: abc123"
        );
    }

    private String artipieCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: artipie",
            "    storage:",
            "      type: fs",
            "      path: any"
        );
    }

    private String artipieCredsWithPolicy() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: artipie",
            "policy:",
            "  type: artipie",
            "  storage:",
            "    type: fs",
            "    path: /any/path"
        );
    }

    private String artipieGithubKeycloakEnvCreds() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: github",
            "  - type: env",
            "  - type: keycloak",
            "    url: http://any",
            "    realm: any",
            "    client-id: any",
            "    client-password: abc123",
            "  - type: artipie",
            "    storage:",
            "      type: fs",
            "      path: any"
        );
    }

    private String artipieGithubKeycloakEnvCredsAndPolicy() {
        return String.join(
            "\n",
            "credentials:",
            "  - type: github",
            "  - type: env",
            "  - type: keycloak",
            "    url: http://any",
            "    realm: any",
            "    client-id: any",
            "    client-password: abc123",
            "  - type: artipie",
            "policy:",
            "  type: artipie",
            "  storage:",
            "    type: fs",
            "    path: /any/path"
        );
    }

}
