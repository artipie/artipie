/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StorageFactory;
import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

/**
 * Factory to create S3 storage.
 *
 * @since 0.1
 */
@ArtipieStorageFactory("s3")
public final class S3StorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final Config cfg) {
        return new S3Storage(
            S3StorageFactory.s3Client(cfg),
            new Config.StrictStorageConfig(cfg)
                .string("bucket"),
            !"false".equals(cfg.string("multipart")),
            endpoint(cfg).orElse("def endpoint")
        );
    }

    /**
     * Creates {@link S3AsyncClient} instance based on YAML config.
     *
     * @param cfg Storage config.
     * @return Built S3 client.
         */
    private static S3AsyncClient s3Client(final Config cfg) {
        final S3AsyncClientBuilder builder = S3AsyncClient.builder();
        Optional.ofNullable(cfg.string("region")).ifPresent(val -> builder.region(Region.of(val)));
        endpoint(cfg).ifPresent(val -> builder.endpointOverride(URI.create(val)));
        setCredentialsProvider(builder, cfg);
        return builder.build();
    }

    /**
     * Sets a credentials provider into the passed builder.
     *
     * @param builder Builder.
     * @param cfg S3 storage configuration.
     */
    private static void setCredentialsProvider(
        final S3AsyncClientBuilder builder,
        final Config cfg
    ) {
        final Config credentials = cfg.config("credentials");
        if (!credentials.isEmpty()) {
            final String type = credentials.string("type");
            if ("basic".equals(type)) {
                builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            credentials.string("accessKeyId"),
                            credentials.string("secretAccessKey")
                        )
                    )
                );
            } else {
                throw new IllegalArgumentException(
                    String.format("Unsupported S3 credentials type: %s", type)
                );
            }
        }
    }

    /**
     * Obtain endpoint from storage config. The parameter is optional.
     *
     * @param cfg Storage config
     * @return Endpoint value is present
     */
    private static Optional<String> endpoint(final Config cfg) {
        return Optional.ofNullable(cfg.string("endpoint"));
    }
}
