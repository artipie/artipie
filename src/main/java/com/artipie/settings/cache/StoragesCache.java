/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.settings.Settings;

/**
 * Cache for storages with similar configurations in Artipie settings.
 * @since 0.23
 */
public interface StoragesCache {
    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param settings Artipie settings
     * @return Storage
     */
    Storage storage(Settings settings);

    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     *
     * @param yaml Storage settings
     * @return Storage
     */
    Storage storage(YamlMapping yaml);

    /**
     * Returns the approximate number of entries in this cache.
     *
     * @return Number of entries
     */
    long size();

    /**
     * Invalidate all items in cache.
     */
    void invalidateAll();
}
