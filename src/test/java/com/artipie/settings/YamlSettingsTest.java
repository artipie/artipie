/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.SubStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link YamlSettings}.
 *
 * @since 0.1
 * @checkstyle MethodNameCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class YamlSettingsTest {

    @Test
    void shouldSetFlatAsDefaultLayout() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  storage:\n"
                )
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.layout(),
            new IsInstanceOf(Layout.Flat.class)
        );
    }

    @Test
    void shouldBeOrgLayout() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  storage: []\n",
                    "  layout: org\n"
                )
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.layout(),
            new IsInstanceOf(Layout.Org.class)
        );
    }

    @Test
    void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path")
        );
        MatcherAssert.assertThat(
            settings.configStorage(),
            Matchers.notNullValue()
        );
    }

    @Test
    void returnsRepoConfigs(@TempDir final Path tmp) {
        MatcherAssert.assertThat(
            new YamlSettings(
                this.config(tmp.toString())
            ).repoConfigsStorage(),
            new IsInstanceOf(SubStorage.class)
        );
    }

    @ParameterizedTest
    @MethodSource("badYamls")
    void shouldFailProvideStorageFromBadYaml(final String yaml) throws IOException {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(yaml).readYamlMapping()
        );
        Assertions.assertThrows(RuntimeException.class, settings::configStorage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "meta:\n"})
    void throwsErrorIfMetaSectionIsAbsentOrEmpty(final String yaml) {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new YamlSettings(Yaml.createYamlInput(yaml).readYamlMapping()).meta()
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> badYamls() {
        return Stream.of(
            "meta:\n  storage:\n",
            "meta:\n  storage:\n    type: unknown\n",
            "meta:\n  storage:\n    type: fs\n",
            "meta:\n  storage:\n    type: s3\n",
            String.join(
                "",
                "meta:\n",
                "  storage:\n",
                "    type: s3\n",
                "    bucket: my-bucket\n",
                "    region: my-region\n",
                "    endpoint: https://my-s3-provider.com\n",
                "    credentials:\n",
                "      type: unknown\n"
            )
        );
    }

    private YamlMapping config(final String stpath) {
        return Yaml.createYamlMappingBuilder()
            .add(
                "meta",
                Yaml.createYamlMappingBuilder()
                    .add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", stpath).build()
                    )
                    .add("repo_configs", "repos")
                    .build()
            ).build();
    }
}
