/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.error;

import java.util.Optional;

/**
 * If a blob upload has been cancelled or was never started, this error code may be returned.
 *
 * @since 0.5
 */
public final class UploadUnknownError implements DockerError {

    /**
     * Upload UUID.
     */
    private final String uuid;

    /**
     * Ctor.
     *
     * @param uuid Upload UUID.
     */
    public UploadUnknownError(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String code() {
        return "BLOB_UPLOAD_UNKNOWN";
    }

    @Override
    public String message() {
        return "blob upload unknown to registry";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.uuid);
    }
}
