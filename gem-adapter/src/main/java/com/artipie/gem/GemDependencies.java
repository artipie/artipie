/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;

/**
 * Gem repository provides dependencies info in custom binary format.
 * User can request dependencies for multiple gems
 * and receive merged result for dependencies info.
 *
 * @since 1.3
 */
public interface GemDependencies {

    /**
     * Find dependencies for gems provided.
     * @param gems Set of gem paths
     * @return Binary dependencies data
     */
    ByteBuffer dependencies(Set<? extends Path> gems);
}
