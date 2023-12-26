/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteStreams;
import java.net.URI;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Tests for {@link Bucket}.
 *
 * @since 0.1
 */
@DisabledOnOs(OS.WINDOWS)
class BucketTest {

    /**
     * Mock S3 server.
     */
    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket name to use in tests.
     */
    private String name;

    /**
     * Bucket instance being tested.
     */
    private Bucket bucket;

    @BeforeEach
    void setUp(final AmazonS3 client) {
        this.name = UUID.randomUUID().toString();
        client.createBucket(this.name);
        this.bucket = new Bucket(
            S3AsyncClient.builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
                )
                .endpointOverride(
                    URI.create(String.format("http://localhost:%d", MOCK.getHttpPort()))
                )
                .build(),
            this.name
        );
    }

    @Test
    void shouldUploadPartAndCompleteMultipartUpload(final AmazonS3 client) throws Exception {
        final String key = "multipart";
        final String id = client.initiateMultipartUpload(
            new InitiateMultipartUploadRequest(this.name, key)
        ).getUploadId();
        final byte[] data = "data".getBytes();
        this.bucket.uploadPart(
            UploadPartRequest.builder()
                .key(key)
                .uploadId(id)
                .partNumber(1)
                .contentLength((long) data.length)
                .build(),
            AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(data))
        ).thenCompose(
            uploaded -> this.bucket.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .key(key)
                    .uploadId(id)
                    .multipartUpload(
                        CompletedMultipartUpload.builder()
                            .parts(CompletedPart.builder()
                                .partNumber(1)
                                .eTag(uploaded.eTag())
                                .build()
                            )
                            .build()
                    )
                    .build()
            )
        ).join();
        final byte[] downloaded;
        try (S3Object s3Object = client.getObject(this.name, key)) {
            downloaded = ByteStreams.toByteArray(s3Object.getObjectContent());
        }
        MatcherAssert.assertThat(downloaded, new IsEqual<>(data));
    }

    @Test
    void shouldAbortMultipartUploadWhenFailedToReadContent(final AmazonS3 client) {
        final String key = "abort";
        final String id = client.initiateMultipartUpload(
            new InitiateMultipartUploadRequest(this.name, key)
        ).getUploadId();
        final byte[] data = "abort_test".getBytes();
        this.bucket.uploadPart(
            UploadPartRequest.builder()
                .key(key)
                .uploadId(id)
                .partNumber(1)
                .contentLength((long) data.length)
                .build(),
            AsyncRequestBody.fromPublisher(AsyncRequestBody.fromBytes(data))
        ).thenCompose(
            ignore -> this.bucket.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                    .key(key)
                    .uploadId(id)
                    .build()
            )
        ).join();
        MatcherAssert.assertThat(
            client.listMultipartUploads(
                new ListMultipartUploadsRequest(this.name)
            ).getMultipartUploads(),
            new IsEmptyIterable<>()
        );
    }
}
