/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
 * Tests for {@link YamlStorage}.
 *
 * @checkstyle MethodNameCheck (500 lines)
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlStorageTest {

    @Test
    public void shouldBuildFileStorageFromSettings() throws Exception {
        final YamlStorage settings = new YamlStorage(
            Yaml.createYamlInput("type: fs\npath: /artipie/storage\n").readYamlMapping()
        );
        MatcherAssert.assertThat(
            settings.storage(),
            Matchers.instanceOf(FileStorage.class)
        );
    }

    @Test
    public void shouldBuildS3StorageFromFullSettings() throws Exception {
        final YamlStorage settings = new YamlStorage(
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
        final YamlStorage settings = new YamlStorage(
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
        final YamlStorage settings = new YamlStorage(
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
