/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.CachedYamlPolicy;
import com.artipie.security.policy.Policy;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

/**
 * Test for {@link ArtipieSecurity.FromYaml}.
 */
class ArtipieSecurityTest {

    private static final Authentication AUTH = (username, password) -> Optional.empty();

    @Test
    void initiatesPolicy() throws IOException {
        final ArtipieSecurity security = new ArtipieSecurity.FromYaml(
            Yaml.createYamlInput(this.policy()).readYamlMapping(),
            ArtipieSecurityTest.AUTH, Optional.empty()
        );
        Assertions.assertInstanceOf(
            ArtipieSecurityTest.AUTH.getClass(), security.authentication()
        );
        MatcherAssert.assertThat(
            "Returns provided empty optional",
            security.policyStorage().isEmpty()
        );
        Assertions.assertInstanceOf(CachedYamlPolicy.class, security.policy());
    }

    @Test
    void returnsFreePolicyIfYamlSectionIsAbsent() {
        MatcherAssert.assertThat(
            "Initiates policy",
            new ArtipieSecurity.FromYaml(
                Yaml.createYamlMappingBuilder().build(),
                ArtipieSecurityTest.AUTH, Optional.empty()
            ).policy(),
            new IsInstanceOf(Policy.FREE.getClass())
        );
    }

    private String policy() {
        return String.join(
            "\n",
            "policy:",
            "  type: artipie",
            "  storage:",
            "    type: fs",
            "    path: /any/path"
        );
    }

}
