/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthFromKeycloakFactory}.
 * @since 0.30
 */
class AuthFromKeycloakFactoryTest {

    @Test
    void initsKeycloak() throws IOException {
        MatcherAssert.assertThat(
            new AuthFromKeycloakFactory().getAuthentication(
                Yaml.createYamlInput(this.artipieKeycloakEnvCreds()).readYamlMapping()
            ),
            new IsInstanceOf(AuthFromKeycloak.class)
        );
    }

    private String artipieKeycloakEnvCreds() {
        return String.join(
            "\n",
            "credentials:",
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

}
