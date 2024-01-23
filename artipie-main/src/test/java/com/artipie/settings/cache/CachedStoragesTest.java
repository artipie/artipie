/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.scheduling.QuartzService;
import com.artipie.settings.Settings;
import com.artipie.settings.YamlSettings;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link CachedStorages}.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedStoragesTest {

    /**
     * Test directory.
         */
    @TempDir
    Path temp;

    @Test
    void getsValueFromCache() {
        final String path = "same/path/for/storage";
        final CachedStorages cache = new CachedStorages();
        final Storage strg = cache.storage(this.config(path));
        final Storage same = cache.storage(this.config(path));
        MatcherAssert.assertThat(
            "Obtained configurations were different",
            strg.equals(same),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsOriginForDifferentConfiguration() {
        final CachedStorages cache = new CachedStorages();
        final Storage frst = cache.storage(this.config("first"));
        final Storage scnd = cache.storage(this.config("second"));
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Storage configuration was not cached",
            cache.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void failsToGetStorageWhenSectionIsAbsent() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new CachedStorages().storage(
                new YamlSettings(
                    Yaml.createYamlMappingBuilder()
                        .add("meta", Yaml.createYamlMappingBuilder().build())
                        .build(), Path.of("a/b/c"), new QuartzService()
                )
            )
        );
    }

    private Settings config(final String stpath) {
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
                ).build(), this.temp, new QuartzService()
        );
    }
}
