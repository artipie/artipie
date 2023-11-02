/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.hex;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Class for working with resources.
 *
 * @since 0.1
 */
public final class ResourceUtil {
    /**
     * File name.
     */
    private final String name;

    /**
     * Ctor.
     *
     * @param name File name
     */
    public ResourceUtil(final String name) {
        this.name = name;
    }

    /**
     * Obtains resources from context loader.
     *
     * @return File path
     */
    public Path asPath() {
        try {
            return Paths.get(
                Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(this.name)
                ).toURI()
            );
        } catch (final URISyntaxException error) {
            throw new IllegalStateException("Failed to obtain test recourse", error);
        }
    }
}
