/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.settings.Settings;
import com.artipie.settings.YamlSettings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CachedStorages}.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedStoragesTest {
    /**
     * Cache for users.
     */
    private Cache<CachedStorages.Metadata, Storage> cache;

    @BeforeEach
    void setUp() {
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
            .softValues().build();
    }

    @Test
    void getsValueFromCache() {
        final String path = "same/path/for/storage";
        final CachedStorages storages = new CachedStorages(this.cache);
        final Storage strg = storages.storage(CachedStoragesTest.config(path));
        final Storage same = storages.storage(CachedStoragesTest.config(path));
        MatcherAssert.assertThat(
            "Obtained configurations were different",
            strg.equals(same),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            this.cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsOriginForDifferentConfiguration() {
        final CachedStorages storages = new CachedStorages(this.cache);
        final Storage frst = storages.storage(CachedStoragesTest.config("first"));
        final Storage scnd = storages.storage(CachedStoragesTest.config("second"));
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            this.cache.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void failsToGetStorageWhenSectionIsAbsent() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new CachedStorages(this.cache).storage(
                new YamlSettings(
                    Yaml.createYamlMappingBuilder()
                        .add("meta", Yaml.createYamlMappingBuilder().build())
                        .build(),
                    new SettingsCaches.Fake()
                )
            )
        );
    }

    private static Settings config(final String stpath) {
        return new YamlSettings(
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", stpath).build()
                    ).build()
                ).build(),
            new SettingsCaches.Fake()
        );
    }
}
