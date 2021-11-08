/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import com.artipie.Settings;
import com.artipie.YamlSettings;
import com.artipie.management.Users;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link CachedCreds}.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedCredsTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "/some/path/with/prefix", "only/some/path"
    })
    void getsValueFromCache(final String path) {
        final String stpath = "any/storage/path";
        final CredsConfigCache cache = new CachedCreds();
        cache.invalidateAll();
        final Users creds = cache.credentials(CachedCredsTest.config(path, stpath));
        final Users same = cache.credentials(CachedCredsTest.config(path, stpath));
        MatcherAssert.assertThat(
            "Obtained configurations were different",
            creds.equals(same),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            cache.toString(),
            new StringContains("size=1")
        );
    }

    @Test
    void getsOriginForDifferentConfiguration() {
        final String stpath = "any/storage/path/also";
        final CredsConfigCache cache = new CachedCreds();
        cache.invalidateAll();
        final Users frst = cache.credentials(CachedCredsTest.config("first", stpath));
        final Users scnd = cache.credentials(CachedCredsTest.config("second", stpath));
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            cache.toString(),
            new StringContains("size=2")
        );
    }

    @Test
    void failsToGetCredentialsWhenSectionIsAbsent() {
        final CredsConfigCache cache = new CachedCreds();
        cache.invalidateAll();
        Assertions.assertThrows(
            ArtipieException.class,
            () -> cache.credentials(
                new YamlSettings(
                    Yaml.createYamlMappingBuilder()
                        .add("meta", Yaml.createYamlMappingBuilder().build())
                        .build(),
                    new SettingsCaches.Fake()
                )
            )
        );
    }

    @Test
    void getsOriginForSameConfigurationButDifferentStorages() {
        final String crpath = "any/creds/path";
        final CredsConfigCache cache = new CachedCreds();
        cache.invalidateAll();
        final Users frst = cache.credentials(CachedCredsTest.config(crpath, "first/strg"));
        final Users scnd = cache.credentials(CachedCredsTest.config(crpath, "second/strg"));
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            cache.toString(),
            new StringContains("size=2")
        );
    }

    @Test
    void failsToGetCredentialsWhenPathIsAbsent() {
        final CredsConfigCache cache = new CachedCreds();
        cache.invalidateAll();
        Assertions.assertThrows(
            ArtipieException.class,
            () -> cache.credentials(
                new YamlSettings(
                    Yaml.createYamlMappingBuilder()
                        .add(
                            "meta", Yaml.createYamlMappingBuilder()
                                .add("credentials", Yaml.createYamlMappingBuilder().build())
                                .build()
                        ).build(),
                    new SettingsCaches.Fake()
                )
            )
        );
    }

    private static Settings config(final String crpath, final String stpath) {
        return new YamlSettings(
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "credentials",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "file")
                            .add("path", crpath).build()
                    ).add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", stpath)
                            .build()
                    ).build()
                ).build(),
            new SettingsCaches.All()
        );
    }
}
