/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.Cleanable;
import com.artipie.settings.cache.StoragesCache;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test Artipie caches.
 * @since 0.28
 */
public final class TestArtipieCaches implements ArtipieCaches {

    /**
     * Cache for configurations of storages.
     */
    private final StoragesCache strgcache;

    /**
     * Was invalidating method called?
     */
    private final AtomicLong invalidation;

    /**
     * Ctor with all fake initialized caches.
     */
    public TestArtipieCaches() {
        this.strgcache = new TestStoragesCache();
        this.invalidation = new AtomicLong();
    }

    @Override
    public StoragesCache storagesCache() {
        return this.strgcache;
    }

    @Override
    public Cleanable usersCache() {
        return this.invalidation::incrementAndGet;
    }

    /**
     * True if invalidate method of the {@link Cleanable} was called exactly one time.
     * @return True if invalidated
     */
    public boolean wasInvalidated() {
        return this.invalidation.get() == 1;
    }
}
