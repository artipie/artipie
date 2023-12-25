/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.s3;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * S3 client targeted at specific bucket.
 *
 * @since 0.1
 */
final class Bucket {

    /**
     * S3 client.
     */
    private final S3AsyncClient client;

    /**
     * Bucket name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param client S3 client.
     * @param name Bucket name.
     */
    Bucket(final S3AsyncClient client, final String name) {
        this.client = client;
        this.name = name;
    }

    /**
     * Handles {@link UploadPartResponse}.
     * See {@link S3AsyncClient#uploadPart(UploadPartRequest, AsyncRequestBody)}
     *
     * @param request Request to bucket.
     * @param body Part body to upload.
     * @return Response to request.
     */
    public CompletableFuture<UploadPartResponse> uploadPart(
        final UploadPartRequest request,
        final AsyncRequestBody body) {
        return this.client.uploadPart(request.copy(original -> original.bucket(this.name)), body);
    }

    /**
     * Handles {@link CompleteMultipartUploadRequest}.
     * See {@link S3AsyncClient#completeMultipartUpload(CompleteMultipartUploadRequest)}
     *
     * @param request Request to bucket.
     * @return Response to request.
     */
    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        final CompleteMultipartUploadRequest request) {
        return this.client.completeMultipartUpload(
            request.copy(original -> original.bucket(this.name))
        );
    }

    /**
     * Handles {@link AbortMultipartUploadRequest}.
     * See {@link S3AsyncClient#abortMultipartUpload(AbortMultipartUploadRequest)}
     *
     * @param request Request to bucket.
     * @return Response to request.
     */
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(
        final AbortMultipartUploadRequest request) {
        return this.client.abortMultipartUpload(
            request.copy(original -> original.bucket(this.name))
        );
    }
}
