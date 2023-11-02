/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.error;

import java.util.Optional;

/**
 * Docker registry error.
 * See <a href="https://docs.docker.com/registry/spec/api/#errors">Errors</a>.
 * Full list of errors could be found
 * <a href="https://docs.docker.com/registry/spec/api/#errors-2">here</a>.
 *
 * @since 0.5
 */
public interface DockerError {

    /**
     * Get code.
     *
     * @return Code identifier string.
     */
    String code();

    /**
     * Get message.
     *
     * @return Message describing conditions.
     */
    String message();

    /**
     * Get detail.
     *
     * @return Unstructured details, might be absent.
     */
    Optional<String> detail();
}
