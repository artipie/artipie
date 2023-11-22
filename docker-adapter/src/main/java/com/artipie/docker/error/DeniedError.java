/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.error;

import java.util.Optional;

/**
 * The access controller denied access for the operation on a resource.
 *
 * @since 0.5
 */
public final class DeniedError implements DockerError {

    @Override
    public String code() {
        return "DENIED";
    }

    @Override
    public String message() {
        return "requested access to the resource is denied";
    }

    @Override
    public Optional<String> detail() {
        return Optional.empty();
    }
}
