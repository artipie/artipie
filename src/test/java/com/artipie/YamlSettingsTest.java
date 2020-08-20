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
import com.artipie.http.auth.Authentication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * @todo #187:30min Add a test for layout structure, it can be either `flat` or `org`. If it's
 *  omitted, it should treated as a `flat` layout. See RepoLayout javadocs for more details.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlSettingsTest {

    @Test
    public void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
            "meta:\n  storage:\n    type: fs\n    path: /artipie/storage\n"
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    public void shouldBuildS3StorageFromSettings() throws Exception {
        final YamlSettings settings = new YamlSettings(
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
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.notNullValue()
        );
    }

    @Test
    public void shouldCreateAuthFromEnv() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", "some/path").build()
                    ).add(
                        "credentials",
                        Yaml.createYamlMappingBuilder().add("type", "env").build()
                    ).build()
                ).build().toString()
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
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", tmp.toString()).build()
                    ).add(
                        "credentials",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "file")
                            .add("path", fname).build()
                    ).build()
                ).build().toString()
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.auth().toCompletableFuture().get(),
            new IsInstanceOf(Authentication.class)
        );
    }

    @Test
    public void returnsCredentials(@TempDir final Path tmp) throws IOException {
        final String fname = "_cred.yml";
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", tmp.toString()).build()
                    ).add(
                        "credentials",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "file")
                            .add("path", fname).build()
                    ).build()
                ).build().toString()
        );
        final Path yaml = tmp.resolve(fname);
        Files.writeString(yaml, this.credentials());
        MatcherAssert.assertThat(
            settings.credentials().toCompletableFuture().join().get().toString(),
            new IsEqual<>(this.credentials())
        );
    }

    @Test
    public void shouldThrowExceptionWhenPathIsNotSet() throws Exception {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlMappingBuilder()
                .add(
                    "meta",
                    Yaml.createYamlMappingBuilder().add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", "some/path").build()
                    ).add(
                        "credentials",
                        Yaml.createYamlMappingBuilder().add("type", "file").build()
                    ).build()
                ).toString()
        );
        Assertions.assertThrows(
            RuntimeException.class,
            () -> settings.auth().toCompletableFuture().get()
        );
    }

    @ParameterizedTest
    @MethodSource("badYamls")
    public void shouldFailProvideStorageFromBadYaml(final String yaml) {
        final YamlSettings settings = new YamlSettings(yaml);
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
}
