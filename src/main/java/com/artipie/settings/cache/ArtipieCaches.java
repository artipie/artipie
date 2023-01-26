/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.asto.factory.StoragesLoader;

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
     * Obtains cache for configurations of credentials.
     *
     * @return Cache for configurations of credentials.
     */
    CredsConfigCache credsConfig();

    /**
     * Obtains cache for user logins.
     *
     * @return Cache for user logins.
     */
    AuthCache auth();

    /**
     * Implementation with all real instances of caches.
     *
     * @since 0.23
     */
    class All implements ArtipieCaches {
        /**
         * Cache for user logins.
         */
        private final AuthCache authcache;

        /**
         * Cache for configurations of storages.
         */
        private final StoragesCache strgcache;

        /**
         * Cache for configurations of credentials.
         */
        private final CredsConfigCache credscache;

        /**
         * Ctor with all initialized caches.
         */
        public All() {
            this.authcache = new CachedUsers();
            this.strgcache = new CachedStorages(new StoragesLoader());
            this.credscache = new CachedCreds();
        }

        @Override
        public StoragesCache storagesCache() {
            return this.strgcache;
        }

        @Override
        public CredsConfigCache credsConfig() {
            return this.credscache;
        }

        @Override
        public AuthCache auth() {
            return this.authcache;
        }
    }
}
