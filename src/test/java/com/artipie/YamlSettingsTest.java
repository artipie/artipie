/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.SubStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
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
    public void shouldSetFlatAsDefaultLayout() throws Exception {
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
    public void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "env", Optional.empty())
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    public void shouldBuildS3StorageFromSettings() throws Exception {
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
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    public void shouldCreateAuthFromEnv() throws Exception {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "env", Optional.empty())
        );
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get().toString(),
            new StringContains("AuthFromEnv")
        );
    }

    @Test
    public void shouldCreateAuthFromYaml(@TempDir final Path tmp)
        throws Exception {
        final String fname = "_cred.yml";
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), "file", Optional.of(fname))
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get().toString(),
            new StringContains("AuthFromYaml")
        );
    }

    @Test
    public void returnsCredentials(@TempDir final Path tmp) throws IOException {
        final String fname = "_cred.yml";
        final YamlSettings settings = new YamlSettings(
            this.config(tmp.toString(), "file", Optional.of(fname))
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.credentials().toCompletableFuture().join(),
            new IsInstanceOf(UsersFromStorageYaml.class)
        );
    }

    @Test
    public void returnsRepoConfigs(@TempDir final Path tmp) {
        MatcherAssert.assertThat(
            new YamlSettings(this.config(tmp.toString(), "file", Optional.empty()))
                .repoConfigsStorage(),
            new IsInstanceOf(SubStorage.class)
        );
    }

    @Test
    public void shouldThrowExceptionWhenPathIsNotSet() {
        final YamlSettings settings = new YamlSettings(
            this.config("some/path", "file", Optional.empty())
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
    public void shouldFailProvideStorageFromBadYaml(final String yaml) throws IOException {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(yaml).readYamlMapping()
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
