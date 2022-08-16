/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.Settings;

/**
 * Cache for storages with similar configurations in Artipie settings.
 * @since 0.23
 */
public interface StorageConfigCache {
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

    /**
     * Fake implementation of {@link StorageConfigCache} which
     * always return in-memory storage.
     * @since 0.22
     */
    class Fake implements StorageConfigCache {
        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         */
        Fake() {
            this.storage = new InMemoryStorage();
        }

        @Override
        public Storage storage(final Settings ignored) {
            return this.storage;
        }

        @Override
        public void invalidateAll() {
            // do nothing
        }
    }
}
