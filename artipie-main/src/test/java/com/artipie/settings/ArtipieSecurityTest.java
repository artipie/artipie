/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.CachedYamlPolicy;
import com.artipie.security.policy.Policy;
import java.io.IOException;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ArtipieSecurity.FromYaml}.
 * @since 0.29
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class ArtipieSecurityTest {

    @Test
    void initiatesPolicy() throws IOException {
        final ArtipieSecurity security = new ArtipieSecurity.FromYaml(
            Yaml.createYamlInput(this.policy()).readYamlMapping(),
            Authentication.ANONYMOUS, Optional.empty()
        );
        MatcherAssert.assertThat(
            "Returns provided authentication",
            security.authentication(),
            new IsInstanceOf(Authentication.ANONYMOUS.getClass())
        );
        MatcherAssert.assertThat(
            "Returns provided empty optional",
            security.policyStorage().isEmpty()
        );
        MatcherAssert.assertThat(
            "Initiates policy",
            security.policy(),
            new IsInstanceOf(CachedYamlPolicy.class)
        );
    }

    @Test
    void returnsFreePolicyIfYamlSectionIsAbsent() {
        MatcherAssert.assertThat(
            "Initiates policy",
            new ArtipieSecurity.FromYaml(
                Yaml.createYamlMappingBuilder().build(),
                Authentication.ANONYMOUS, Optional.empty()
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
