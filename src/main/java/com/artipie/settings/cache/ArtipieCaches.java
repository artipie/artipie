/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

/**
 * Encapsulates caches which are possible to use in settings of Artipie server.
 *
 * @since 0.23
 */
public interface ArtipieCaches {
    /**
     * Obtains storages cache.
     *
     * @return Storages cache.
     */
    StoragesCache storagesCache();

    /**
     * Obtains cache for user logins.
     *
     * @return Cache for user logins.
     */
    Cleanable usersCache();

    /**
     * Implementation with all real instances of caches.
     *
     * @since 0.23
     */
    class All implements ArtipieCaches {
        /**
         * Cache for user logins.
         */
        private final Cleanable authcache;

        /**
         * Cache for configurations of storages.
         */
        private final StoragesCache strgcache;

        /**
         * Ctor with all initialized caches.
         * @param users Users cache
         * @param strgcache Storages cache
         */
        public All(final Cleanable users, final StoragesCache strgcache) {
            this.authcache = users;
            this.strgcache = strgcache;
        }

        @Override
        public StoragesCache storagesCache() {
            return this.strgcache;
        }

        @Override
        public Cleanable usersCache() {
            return this.authcache;
        }
    }
}
