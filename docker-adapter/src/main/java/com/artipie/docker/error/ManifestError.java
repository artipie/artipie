/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.error;

import com.artipie.docker.ManifestReference;

import java.util.Optional;

/**
 * This error is returned when the manifest, identified by name and tag
 * is unknown to the repository.
 */
public final class ManifestError implements DockerError {

    /**
     * Manifest reference.
     */
    private final ManifestReference ref;

    /**
     * Ctor.
     *
     * @param ref Manifest reference.
     */
    public ManifestError(ManifestReference ref) {
        this.ref = ref;
    }

    @Override
    public String code() {
        return "MANIFEST_UNKNOWN";
    }

    @Override
    public String message() {
        return "manifest unknown";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.ref.digest());
    }
}
