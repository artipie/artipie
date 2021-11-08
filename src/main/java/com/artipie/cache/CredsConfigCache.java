/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.artipie.Settings;
import com.artipie.UsersFromEnv;
import com.artipie.management.Users;

/**
 * Cache for credentials with similar configurations in Artipie settings.
 * @since 0.23
 */
public interface CredsConfigCache {
    /**
     * Finds credentials by specified in settings configuration cache or creates
     * a new item and caches it.
     * @param settings Artipie settings
     * @return Storage
     */
    Users credentials(Settings settings);

    /**
     * Invalidate all items in cache.
     */
    void invalidateAll();

    /**
     * Fake implementation of {@link CredsConfigCache} which
     * always returns credentials from env.
     * @since 0.23
     */
    class FromEnv implements CredsConfigCache {
        /**
         * Users credentials.
         */
        private final Users creds;

        /**
         * Ctor.
         */
        FromEnv() {
            this.creds = new UsersFromEnv();
        }

        @Override
        public Users credentials(final Settings settings) {
            return this.creds;
        }

        @Override
        public void invalidateAll() {
            // do nothing
        }
    }
}
