/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link SettingsFromPath}.
 * @since 0.22
 */
class SettingsFromPathTest {

    @Test
    void createsSettings(final @TempDir Path temp) throws IOException {
        final Path stng = temp.resolve("artipie.yaml");
        Files.write(
            Yaml.createYamlMappingBuilder().add(
                "meta",
                Yaml.createYamlMappingBuilder().add(
                    "storage",
                    Yaml.createYamlMappingBuilder().add("type", "fs")
                        .add("path", temp.resolve("repo").toString()).build()
                ).build()
            ).build().toString().getBytes(),
            stng.toFile()
        );
        final Settings settings = new SettingsFromPath(stng).find();
        MatcherAssert.assertThat(
            settings,
            new IsInstanceOf(YamlSettings.class)
        );
    }
}
