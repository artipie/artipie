/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.redis;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.test.StorageWhiteboxVerification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;

/**
 * Redis storage verification test.
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
@DisabledOnOs(OS.WINDOWS)
public final class RedisStorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * Default redis port.
     */
    private static final int DEF_PORT = 6379;

    /**
     * Redis test container.
     */
    private static GenericContainer<?> redis;

    /**
     * Redis storage.
     */
    private static Storage storage;

    @Override
    protected Storage newStorage() {
        return RedisStorageWhiteboxVerificationTest.storage;
    }

    @BeforeAll
    static void setUp() {
        RedisStorageWhiteboxVerificationTest.redis = new GenericContainer<>("redis:3-alpine")
            .withExposedPorts(RedisStorageWhiteboxVerificationTest.DEF_PORT);
        RedisStorageWhiteboxVerificationTest.redis.start();
        RedisStorageWhiteboxVerificationTest.storage = StoragesLoader.STORAGES.newObject(
            "redis", config(RedisStorageWhiteboxVerificationTest.redis.getFirstMappedPort())
        );
    }

    @AfterAll
    static void tearDown() {
        RedisStorageWhiteboxVerificationTest.redis.stop();
    }

    private static Config config(final Integer port) {
        return new Config.YamlStorageConfig(
            Yaml.createYamlMappingBuilder()
                .add("type", "redis")
                .add(
                    "config",
                    Yaml.createYamlMappingBuilder()
                        .add(
                            "singleServerConfig",
                            Yaml.createYamlMappingBuilder()
                                .add(
                                    "address",
                                    String.format("redis://127.0.0.1:%d", port)
                                ).build()
                        ).build()
                ).build()
        );
    }
}
