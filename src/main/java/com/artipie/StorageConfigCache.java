/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Storage;

/**
 * Cache for storages with similar configurations in Artipie settings.
 * @since 0.23
 */
interface StorageConfigCache {
    /**
     * Finds storage by specified in settings configuration cache or creates
     * a new item and caches it.
     * @param settings Artipie settings
     * @return Storage
     */
    Storage storage(Settings settings);

    /**
     * Invalidate all items in cache.
     */
    void invalidateAll();
}
