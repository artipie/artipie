/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.SubStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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
            this.config("some/path", this.credentials("env"))
        );
        MatcherAssert.assertThat(
            settings.configStorage(),
            Matchers.notNullValue()
        );
    }

    @Test
    void shouldCreateAuthFromEnv() {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", this.credentials("env"))
        );
        MatcherAssert.assertThat(
            settings.auth().toString(),
            new StringContains("AuthFromEnv")
        );
    }

    @Test
    void shouldCreateGithub() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", this.credentials("github"))
        );
        MatcherAssert.assertThat(
            settings.auth().toString(),
            new StringContains("GithubAuth")
        );
    }

    @Test
    void shouldCreateAuthFromEnvWithAlternativeAuths(@TempDir final Path tmp)
        throws Exception {
        final String fname = "_cred.yml";
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        final YamlSettings settings = new YamlSettings(
            this.config(
                tmp.toString(),
                Yaml.createYamlSequenceBuilder()
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("type", "file")
                            .add("path", fname)
                            .build()
                    )
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("type", "env")
                            .build()
                    )
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("type", "github")
                            .build()
                    ).build()
            )
        );
        MatcherAssert.assertThat(
            settings.auth().toString(),
            Matchers.allOf(
                new StringContains("AuthFromEnv"),
                new StringContains("GithubAuth"),
                new StringContains("AuthFromYaml")
            )
        );
    }

    @Test
    void doNotAuthorizeWithoutCredentialsSection() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlMappingBuilder().add(
                "meta",
                Yaml.createYamlMappingBuilder().add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", "path/storage").build()
                ).build()
            ).build()
        );
        MatcherAssert.assertThat(
            settings.auth().user("any_name", "any_password").isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void shouldCreateAuthFromYaml(@TempDir final Path tmp)
        throws Exception {
        final String fname = "_cred.yml";
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), this.credentials("file", Optional.of(fname)))
        );
        MatcherAssert.assertThat(
            settings.auth().toString(),
            new StringContains("AuthFromYaml")
        );
    }

    @Test
    void getsCredentialsFromCache(@TempDir final Path tmp) throws IOException {
        final String fname = "_cred.yml";
        Files.writeString(tmp.resolve(fname), this.credentials());
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), this.credentials("file", Optional.of(fname)))
        );
        MatcherAssert.assertThat(
            "User was authenticated",
            settings.auth().user("john", "123").isPresent()
        );
        Files.writeString(tmp.resolve(fname), "not valid yaml file");
        MatcherAssert.assertThat(
            "User was not cached",
            settings.caches().usersCache().toString(),
            new StringContains("size=1")
        );
        MatcherAssert.assertThat(
            "Invalid yaml file was used, although credentials from cache should be used",
            settings.auth().user("john", "123").isPresent()
        );
    }

    @Test
    void returnsRepoConfigs(@TempDir final Path tmp) {
        MatcherAssert.assertThat(
            new YamlSettings(
                this.config(tmp.toString(), this.credentials("file", Optional.of("any/path")))
            ).repoConfigsStorage(),
            new IsInstanceOf(SubStorage.class)
        );
    }

    @Test
    void shouldThrowExceptionWhenPathIsNotSet() {
        Assertions.assertThrows(
            RuntimeException.class,
            () -> new YamlSettings(
                this.config("some/path", this.credentials("file", Optional.empty()))
            )
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

    private String credentials() {
        return Yaml.createYamlMappingBuilder().add(
            "credentials",
            Yaml.createYamlMappingBuilder()
                .add(
                    "john",
                    Yaml.createYamlMappingBuilder().add("pass", "plain:123").build()
                ).build()
        ).build().toString();
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

    private YamlNode credentials(final String type) {
        return this.credentials(type, Optional.empty());
    }

    private YamlNode credentials(
        final String type,
        final Optional<String> path
    ) {
        return path.map(
            val -> Yaml.createYamlMappingBuilder()
                .add("type", type)
                .add("path", val)
        ).orElse(Yaml.createYamlMappingBuilder().add("type", type)).build();
    }

    private YamlMapping config(
        final String stpath,
        final YamlNode creds
    ) {
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
                    .add("credentials", creds)
                    .add("repo_configs", "repos")
                    .build()
            ).build();
    }
}
