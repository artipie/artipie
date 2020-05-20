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
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.s3.S3Storage;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for {@link YamlStorageSettings}.
 *
 * @checkstyle MethodNameCheck (500 lines)
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlStorageSettingsTest {

    @Test
    public void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlStorageSettings settings = new YamlStorageSettings(
            Yaml.createYamlInput("type: fs\npath: /artipie/storage\n").readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.instanceOf(FileStorage.class)
        );
    }

    @Test
    public void shouldBuildS3StorageFromFullSettings() throws Exception {
        final YamlStorageSettings settings = new YamlStorageSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "type: s3\n",
                    "bucket: my-bucket\n",
                    "region: my-region\n",
                    "endpoint: https://my-s3-provider.com\n",
                    "credentials:\n",
                    "  type: basic\n",
                    "  accessKeyId: ***\n",
                    "  secretAccessKey: ***\n"
                )
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.instanceOf(S3Storage.class)
        );
    }

    @Test
    public void shouldBuildS3StorageFromMinimalSettings() throws Exception {
        System.getProperties().put("aws.region", "my-region");
        final YamlStorageSettings settings = new YamlStorageSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "type: s3\n",
                    "bucket: my-bucket\n",
                    "credentials:\n",
                    "  type: basic\n",
                    "  accessKeyId: ***\n",
                    "  secretAccessKey: ***\n"
                )
            ).readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.instanceOf(S3Storage.class)
        );
    }

    @ParameterizedTest
    @MethodSource("badYamls")
    public void shouldFailProvideStorageFromBadYaml(final String yaml)
        throws Exception {
        final YamlStorageSettings settings = new YamlStorageSettings(
            Yaml.createYamlInput(yaml).readYamlMapping()
        );
        Assertions.assertThrows(RuntimeException.class, settings::storage);
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> badYamls() {
        return Stream.of(
            "",
            "type: unknown\n",
            "type: fs\n",
            "type: s3\n",
            String.join(
                "",
                "type: s3\n",
                "bucket: my-bucket\n",
                "region: my-region\n",
                "endpoint: https://my-s3-provider.com\n",
                "credentials:\n",
                "  type: unknown\n"
            )
        );
    }
}
