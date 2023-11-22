/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.error;

import com.artipie.docker.Digest;
import java.util.Optional;

/**
 * This error may be returned when a blob is unknown to the registry in a specified repository.
 * This can be returned with a standard get
 * or if a manifest references an unknown layer during upload.
 *
 * @since 0.5
 */
public final class BlobUnknownError implements DockerError {

    /**
     * Blob digest.
     */
    private final Digest digest;

    /**
     * Ctor.
     *
     * @param digest Blob digest.
     */
    public BlobUnknownError(final Digest digest) {
        this.digest = digest;
    }

    @Override
    public String code() {
        return "BLOB_UNKNOWN";
    }

    @Override
    public String message() {
        return "blob unknown to registry";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.digest.string());
    }
}
