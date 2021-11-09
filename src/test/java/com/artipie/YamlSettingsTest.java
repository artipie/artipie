/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.SubStorage;
import com.artipie.cache.SettingsCaches;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
            ).readYamlMapping(),
            new SettingsCaches.Fake()
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
            ).readYamlMapping(),
            new SettingsCaches.Fake()
        );
        MatcherAssert.assertThat(
            settings.layout(),
            new IsInstanceOf(Layout.Org.class)
        );
    }

    @Test
    void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "env", Optional.empty()),
            new SettingsCaches.All()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    void shouldBuildS3StorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  storage:\n",
                    "    type: s3\n",
                    "    bucket: my-bucket\n",
                    "    region: my-region\n",
                    "    endpoint: https://my-s3-provider.com\n",
                    "    credentials:\n",
                    "      type: basic\n",
                    "      accessKeyId: ***\n",
                    "      secretAccessKey: ***"
                )
            ).readYamlMapping(),
            new SettingsCaches.All()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    void shouldCreateAuthFromEnv() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "env", Optional.empty()),
            new SettingsCaches.All()
        );
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get().toString(),
            new StringContains("AuthFromEnv")
        );
    }

    @Test
    void authorizesWithoutCredentialsSection() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlMappingBuilder().add(
                "meta",
                Yaml.createYamlMappingBuilder().add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", "path/storage").build()
                ).build()
            ).build(),
            new SettingsCaches.All()
        );
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get()
                .user("any_name", "any_password")
                .get().name(),
            new IsEqual<>("anonymous")
        );
    }

    @Test
    void shouldCreateAuthFromYaml(@TempDir final Path tmp)
        throws Exception {
        final String fname = "_cred.yml";
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), "file", Optional.of(fname)),
            new SettingsCaches.All()
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get().toString(),
            new StringContains("AuthFromYaml")
        );
    }

    @Test
    void returnsCredentials(@TempDir final Path tmp) throws IOException {
        final String fname = "_cred.yml";
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), "file", Optional.of(fname)),
            new SettingsCaches.All()
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.credentials().toCompletableFuture().join(),
            new IsInstanceOf(UsersFromStorageYaml.class)
        );
    }

    @Test
    void returnsRepoConfigs(@TempDir final Path tmp) {
        MatcherAssert.assertThat(
            new YamlSettings(
                this.config(tmp.toString(), "file", Optional.empty()),
                new SettingsCaches.Fake()
            ).repoConfigsStorage(),
            new IsInstanceOf(SubStorage.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenPathIsNotSet() {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "file", Optional.empty()),
            new SettingsCaches.Fake()
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ExecutionException.class,
                () -> settings.auth().toCompletableFuture().get()
            ).getCause(),
            new IsInstanceOf(RuntimeException.class)
        );
    }

    @ParameterizedTest
    @MethodSource("badYamls")
    void shouldFailProvideStorageFromBadYaml(final String yaml) throws IOException {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(yaml).readYamlMapping(),
            new SettingsCaches.All()
        );
        Assertions.assertThrows(RuntimeException.class, settings::storage);
    }

    private String credentials() {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(
                    "john",
                    Yaml.createYamlMappingBuilder().add("password", "plain:123").build()
                ).build()
        ).build().toString();
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> badYamls() {
        return Stream.of(
            "",
            "meta:\n",
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

    private YamlMapping config(final String stpath, final String type,
        final Optional<String> path) {
        final YamlMappingBuilder creds = path.map(
            val -> Yaml.createYamlMappingBuilder().add("type", type).add("path", val)
        ).orElse(Yaml.createYamlMappingBuilder().add("type", type));
        return Yaml.createYamlMappingBuilder()
            .add(
                "meta",
                Yaml.createYamlMappingBuilder().add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", stpath).build()
                )
                    .add("credentials", creds.build())
                    .add("repo_configs", "repos")
                    .build()
            ).build();
    }
}
