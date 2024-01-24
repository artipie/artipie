/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.artipie.asto.s3.S3Storage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * S3 storage verification test.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
@DisabledOnOs(OS.WINDOWS)
public final class S3StorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * S3 mock server extension.
     */
    private static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false).build();

    @Override
    protected Storage newStorage() {
        final String endpoint = String.format("http://localhost:%d", MOCK.getHttpPort());
        final S3AsyncClient client = S3AsyncClient.builder()
            .region(Region.of("us-east-1"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("foo", "bar")
                )
            )
            .endpointOverride(URI.create(endpoint))
            .build();
        final String bucket = UUID.randomUUID().toString();
        client.createBucket(CreateBucketRequest.builder().bucket(bucket).build()).join();
        return new S3Storage(client, bucket, endpoint);
    }

    @BeforeAll
    static void setUp() throws Exception {
        S3StorageWhiteboxVerificationTest.MOCK.beforeAll(null);
    }

    @AfterAll
    static void tearDown() {
        S3StorageWhiteboxVerificationTest.MOCK.afterAll(null);
    }

}
