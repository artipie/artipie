/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import java.nio.file.Path;

/**
 * Gem repository index.
 *
 * @since 1.0
 */
public interface GemIndex {

    /**
     * Update index.
     * @param path Repository index path
     */
    void update(Path path);
}
