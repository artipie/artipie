/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import com.amihaiemil.eoyaml.Yaml;
import java.time.Duration;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CondaConfig}.
 * @since 0.23
 * @checkstyle MagicNumberCheck (500 lines)
 */
class CondaConfigTest {

    @Test
    void returnsProvidedTtl() {
        MatcherAssert.assertThat(
            new CondaConfig(
                Optional.of(Yaml.createYamlMappingBuilder().add("auth_token_ttl", "P3D").build())
            ).authTokenTtl(),
            new IsEqual<>(Duration.ofDays(3))
        );
    }

    @Test
    void returnsDefTtl() {
        MatcherAssert.assertThat(
            new CondaConfig(Optional.empty()).authTokenTtl(),
            new IsEqual<>(Duration.ofDays(365))
        );
    }

    @Test
    void returnsProvidedCleanCronExp() {
        final String value = "0 0 12 * * ?";
        MatcherAssert.assertThat(
            new CondaConfig(
                Optional.of(
                    Yaml.createYamlMappingBuilder()
                        .add("clean_auth_token_at", value).build()
                )
            ).cleanAuthTokens(),
            new IsEqual<>(value)
        );
    }

    @Test
    void returnsDefCleanCronExp() {
        MatcherAssert.assertThat(
            new CondaConfig(Optional.empty()).cleanAuthTokens(),
            new IsEqual<>("0 0 1 * * ?")
        );
    }

}
