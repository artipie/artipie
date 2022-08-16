/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

/**
 * Encapsulates caches which are possible to use in settings of Artipie server.
 * @since 0.23
 */
public interface SettingsCaches {
    /**
     * Obtains cache for configurations of storages.
     * @return Cache for configurations of storages.
     */
    StorageConfigCache storageConfig();

    /**
     * Obtains cache for configurations of credentials.
     * @return Cache for configurations of credentials.
     */
    CredsConfigCache credsConfig();

    /**
     * Obtains cache for user logins.
     * @return Cache for user logins.
     */
    AuthCache auth();

    /**
     * Implementation with all real instances of caches.
     * @since 0.23
     */
    class All implements SettingsCaches {
        /**
         * Cache for user logins.
         */
        private final AuthCache authcache;

        /**
         * Cache for configurations of storages.
         */
        private final StorageConfigCache strgcache;

        /**
         * Cache for configurations of credentials.
         */
        private final CredsConfigCache credscache;

        /**
         * Ctor with all initialized caches.
         */
        public All() {
            this.authcache = new CachedUsers();
            this.strgcache = new CachedStorages();
            this.credscache = new CachedCreds();
        }

        @Override
        public StorageConfigCache storageConfig() {
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

    /**
     * Implementation with fake instances of caches.
     * @since 0.23
     */
    class Fake implements SettingsCaches {
        /**
         * Cache for user logins.
         */
        private final AuthCache authcache;

        /**
         * Cache for configurations of storages.
         */
        private final StorageConfigCache strgcache;

        /**
         * Cache for configurations of credentials.
         */
        private final CredsConfigCache credscache;

        /**
         * Ctor with all fake initialized caches.
         */
        public Fake() {
            this.authcache = new AuthCache.Fake();
            this.strgcache = new StorageConfigCache.Fake();
            this.credscache = new CredsConfigCache.Fake();
        }

        @Override
        public StorageConfigCache storageConfig() {
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
