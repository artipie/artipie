/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for EtcdStorageFactory.
 */
public final class EtcdStorageFactoryTest {
    @Test
    void shouldCreateEtcdStorage() {
        MatcherAssert.assertThat(
            StoragesLoader.STORAGES
                .newObject(
                    "etcd",
                    new Config.YamlStorageConfig(
                        Yaml.createYamlMappingBuilder().add(
                            "connection",
                            Yaml.createYamlMappingBuilder()
                                .add(
                                    "endpoints",
                                    Yaml.createYamlSequenceBuilder()
                                        .add("http://localhost")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                    )
                ),
            new IsInstanceOf(EtcdStorage.class)
        );
    }
}
