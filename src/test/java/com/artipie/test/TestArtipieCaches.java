/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.AuthCache;
import com.artipie.settings.cache.CredsConfigCache;
import com.artipie.settings.cache.StoragesCache;

/**
 * Test Artipie caches.
 * @since 0.28
 */
public final class TestArtipieCaches implements ArtipieCaches {

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
     * Ctor with all fake initialized caches.
     */
    public TestArtipieCaches() {
        this.authcache = new AuthCache.Fake();
        this.strgcache = new TestStoragesCache();
        this.credscache = new CredsConfigCache.Fake();
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
