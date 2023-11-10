/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.error;

import java.util.Optional;

/**
 * Invalid repository name encountered either during manifest validation or any API operation.
 *
 * @since 0.5
 */
@SuppressWarnings("serial")
public final class InvalidRepoNameException extends RuntimeException implements DockerError {

    /**
     * Ctor.
     *
     * @param details Error details.
     */
    public InvalidRepoNameException(final String details) {
        super(details);
    }

    @Override
    public String code() {
        return "NAME_INVALID";
    }

    @Override
    public String message() {
        return "invalid repository name";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.getMessage());
    }
}
