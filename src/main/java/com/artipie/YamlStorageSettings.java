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

import com.amihaiemil.eoyaml.StrictYamlMapping;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.s3.S3Storage;
import java.net.URI;
import java.nio.file.Path;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

/**
 * Storage settings built from YAML.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class YamlStorageSettings {

    /**
     * YAML storage settings.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml YAML storage settings.
     */
    YamlStorageSettings(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Provides a storage.
     *
     * @return Storage instance.
     */
    public Storage storage() {
        final YamlMapping strict = new StrictYamlMapping(this.yaml);
        final String type = strict.string("type");
        final Storage storage;
        if ("fs".equals(type)) {
            storage = new FileStorage(Path.of(strict.string("path")));
        } else if ("s3".equals(type)) {
            storage = new S3Storage(this.s3Client(), strict.string("bucket"));
        } else {
            throw new IllegalStateException(String.format("Unsupported storage type: '%s'", type));
        }
        return storage;
    }

    /**
     * Creates {@link S3AsyncClient} instance based on YAML config.
     *
     * @return Built S3 client.
     * @checkstyle MethodNameCheck (2 lines)
     */
    private S3AsyncClient s3Client() {
        final S3AsyncClientBuilder builder = S3AsyncClient.builder();
        final String region = this.yaml.string("region");
        if (region != null) {
            builder.region(Region.of(region));
        }
        final String endpoint = this.yaml.string("endpoint");
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder
            .credentialsProvider(
                credentials(new StrictYamlMapping(this.yaml).yamlMapping("credentials"))
            )
            .build();
    }

    /**
     * Creates {@link StaticCredentialsProvider} instance based on YAML config.
     *
     * @param yaml Credentials config YAML.
     * @return Credentials provider.
     */
    private static StaticCredentialsProvider credentials(final YamlMapping yaml) {
        final String type = yaml.string("type");
        if ("basic".equals(type)) {
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    yaml.string("accessKeyId"),
                    yaml.string("secretAccessKey")
                )
            );
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported S3 credentials type: %s", type)
            );
        }
    }
}
