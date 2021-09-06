/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.time.Duration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link YamlCacheStorage}.
 * @since 0.23
 */
final class YamlCacheStorageTest {
    @Test
    void returnsDefaultMaxSizeWhenItIsAbsentInConfig() {
        MatcherAssert.assertThat(
            new YamlCacheStorage(new InMemoryStorage()).maxSize(),
            new IsEqual<>(Long.MAX_VALUE)
        );
    }

    @Test
    void returnsDefaultTtlWhenItIsAbsentInConfig() {
        MatcherAssert.assertThat(
            new YamlCacheStorage(new InMemoryStorage()).timeToLive(),
            new IsEqual<>(Duration.ofMillis(Long.MAX_VALUE))
        );
    }

    @Test
    void failsWhenStorageSectionIsAbsent() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new YamlCacheStorage(Yaml.createYamlMappingBuilder().build()).storage()
        );
    }

    @Test
    void readsStorageFromConfig() {
        MatcherAssert.assertThat(
            new YamlCacheStorage(YamlCacheStorageTest.storageBldr().build()).storage(),
            new IsInstanceOf(Storage.class)
        );
    }

    @Test
    void readsMaxSizeFromConfig() {
        final Long maxsize = 123L;
        MatcherAssert.assertThat(
            new YamlCacheStorage(
                YamlCacheStorageTest.storageBldr()
                    .add(YamlCacheStorage.MAX_SIZE, String.valueOf(maxsize))
                    .build()
            ).maxSize(),
            new IsEqual<>(maxsize)
        );
    }

    @Test
    void readsTtlFromConfig() {
        final long ttl = 123L;
        MatcherAssert.assertThat(
            new YamlCacheStorage(
                YamlCacheStorageTest.storageBldr()
                    .add(YamlCacheStorage.TTL, String.valueOf(ttl))
                    .build()
            ).timeToLive(),
            new IsEqual<>(Duration.ofMillis(ttl))
        );
    }

    private static YamlMappingBuilder storageBldr() {
        return Yaml.createYamlMappingBuilder()
            .add(
                "storage",
                Yaml.createYamlMappingBuilder()
                    .add("type", "fs")
                    .add("path", "any/path")
                    .build()
            );
    }
}
