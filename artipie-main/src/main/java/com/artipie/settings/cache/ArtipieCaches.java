/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.asto.misc.Cleanable;
import com.artipie.security.policy.CachedYamlPolicy;
import com.artipie.security.policy.Policy;

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
    Cleanable<String> usersCache();

    /**
     * Obtains cache for user policy.
     *
     * @return Cache for policy.
     */
    Cleanable<String> policyCache();

    /**
     * Obtains filters cache.
     *
     * @return Filters cache.
     */
    FiltersCache filtersCache();

    /**
     * Implementation with all real instances of caches.
     *
     * @since 0.23
     */
    class All implements ArtipieCaches {
        /**
         * Cache for user logins.
         */
        private final Cleanable<String> authcache;

        /**
         * Cache for configurations of storages.
         */
        private final StoragesCache strgcache;

        /**
         * Artipie policy.
         */
        private final Policy<?> policy;

        /**
         * Cache for configurations of filters.
                 */
        private final FiltersCache filtersCache;

        /**
         * Ctor with all initialized caches.
         * @param users Users cache
         * @param strgcache Storages cache
         * @param policy Artipie policy
         * @param filtersCache Filters cache
                         */
        public All(
            final Cleanable<String> users,
            final StoragesCache strgcache,
            final Policy<?> policy,
            final FiltersCache filtersCache
        ) {
            this.authcache = users;
            this.strgcache = strgcache;
            this.policy = policy;
            this.filtersCache = filtersCache;
        }

        @Override
        public StoragesCache storagesCache() {
            return this.strgcache;
        }

        @Override
        public Cleanable<String> usersCache() {
            return this.authcache;
        }

        @Override
        public Cleanable<String> policyCache() {
            final Cleanable<String> res;
            if (this.policy instanceof CachedYamlPolicy) {
                res = (CachedYamlPolicy) this.policy;
            } else {
                res = new Cleanable<>() {
                    @Override
                    public void invalidate(final String any) {
                        //do nothing
                    }

                    @Override
                    public void invalidateAll() {
                        //do nothing
                    }
                };
            }
            return res;
        }

        @Override
        public FiltersCache filtersCache() {
            return this.filtersCache;
        }
    }
}
