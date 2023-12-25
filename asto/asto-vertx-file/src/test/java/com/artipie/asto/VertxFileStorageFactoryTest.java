/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.fs.VertxFileStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for Storages.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class VertxFileStorageFactoryTest {

    @Test
    void shouldCreateVertxFileStorage() {
        MatcherAssert.assertThat(
            new StoragesLoader()
                .newObject(
                    "vertx-file",
                    new Config.YamlStorageConfig(
                        Yaml.createYamlMappingBuilder().add("path", "").build()
                    )
                ),
            new IsInstanceOf(VertxFileStorage.class)
        );
    }
}
