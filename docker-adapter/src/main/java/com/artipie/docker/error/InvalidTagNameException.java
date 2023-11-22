/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.error;

import java.util.Optional;

/**
 * Invalid tag name encountered during a manifest upload or any API operation.
 * See <a href="https://docs.docker.com/registry/spec/api/#errors-2">Errors</a>.
 *
 * @since 0.5
 */
@SuppressWarnings("serial")
public final class InvalidTagNameException extends RuntimeException implements DockerError {

    /**
     * Ctor.
     *
     * @param details Error details.
     */
    public InvalidTagNameException(final String details) {
        super(details);
    }

    @Override
    public String code() {
        return "TAG_INVALID";
    }

    @Override
    public String message() {
        return "invalid tag name";
    }

    @Override
    public Optional<String> detail() {
        return Optional.of(this.getMessage());
    }
}
