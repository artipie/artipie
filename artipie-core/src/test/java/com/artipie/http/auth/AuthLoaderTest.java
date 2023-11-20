/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AuthLoader}.
 * @since 1.3
 */
class AuthLoaderTest {

    @Test
    void loadsFactories() {
        final AuthLoader loader = new AuthLoader(
            Collections.singletonMap(
                AuthLoader.SCAN_PACK, "custom.auth.first;custom.auth.second"
            )
        );
        MatcherAssert.assertThat(
            "first auth was created",
            loader.newObject(
                "first",
                Yaml.createYamlMappingBuilder().build()
            ),
            new IsInstanceOf(Authentication.ANONYMOUS.getClass())
        );
        MatcherAssert.assertThat(
            "second auth was created",
            loader.newObject(
                "second",
                Yaml.createYamlMappingBuilder().build()
            ),
            new IsInstanceOf(Authentication.ANONYMOUS.getClass())
        );
    }

    @Test
    void throwsExceptionIfPermNotFound() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new AuthLoader().newObject(
                "unknown_policy",
                Yaml.createYamlMappingBuilder().build()
            )
        );
    }

    @Test
    void throwsExceptionIfPermissionsHaveTheSameName() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new AuthLoader(
                Collections.singletonMap(
                    AuthLoader.SCAN_PACK, "custom.auth.first;custom.auth.duplicate"
                )
            )
        );
    }

}
