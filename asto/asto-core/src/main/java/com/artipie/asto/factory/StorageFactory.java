/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;

/**
 * Storage factory interface.
 *
 * @since 1.13.0
 */
public interface StorageFactory {

    /**
     * Create new storage.
     *
     * @param cfg Storage configuration.
     * @return Storage
     */
    Storage newStorage(Config cfg);

    /**
     * Create new storage.
     *
     * @param cfg Storage configuration.
     * @return Storage
     */
    default Storage newStorage(YamlMapping cfg) {
        return this.newStorage(
            new Config.YamlStorageConfig(cfg)
        );
    }
}
