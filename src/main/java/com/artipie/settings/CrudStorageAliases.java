/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import java.util.Collection;
import javax.json.JsonObject;

/**
 * Create/Read/Update/Delete storages aliases settings.
 * @since 0.1
 */
public interface CrudStorageAliases {

    /**
     * List artipie storages.
     * @return Collection of {@link Storage} instances
     */
    Collection<? extends Storage> list();

    /**
     * Add storage to artipie storages.
     * @param alias Storage alias
     * @param info Storage settings
     */
    void add(String alias, JsonObject info);

    /**
     * Remove storage from settings.
     * @param alias Storage alias
     */
    void remove(String alias);

    /**
     * Artipie storage.
     * @since 0.1
     */
    interface Storage {

        /**
         * Storage alias.
         * @return Alias
         */
        String alias();

        /**
         * Storage settings.
         * @return Settings in json format
         */
        JsonObject info();
    }
}
