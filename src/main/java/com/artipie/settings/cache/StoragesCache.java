/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.Settings;
import java.util.concurrent.atomic.AtomicInteger;

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
     * Invalidate all items in cache.
     */
    void invalidateAll();

    /**
     * Fake implementation of {@link StoragesCache} which
     * always return in-memory storage.
     * @since 0.22
     */
    class Fake implements StoragesCache {
        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Counter for `invalidateAll()` method calls.
         */
        private final AtomicInteger cnt;

        /**
         * Ctor.
         */
        Fake() {
            this.storage = new InMemoryStorage();
            this.cnt = new AtomicInteger(0);
        }

        @Override
        public Storage storage(final Settings ignored) {
            return this.storage;
        }

        @Override
        public Storage storage(final YamlMapping yaml) {
            return this.storage;
        }

        @Override
        public void invalidateAll() {
            this.cnt.incrementAndGet();
        }

        /**
         * Was this case invalidated?
         * @return True, if it was invalidated once
         */
        public boolean wasInvalidated() {
            return this.cnt.get() == 1;
        }

    }
}
