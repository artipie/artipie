/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.asto.misc.Cleanable;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.FiltersCache;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.NotImplementedException;

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
     * Was users invalidating method called?
     */
    private final AtomicLong cleanuser;

    /**
     * Was policy invalidating method called?
     */
    private final AtomicLong cleanpolicy;

    /**
     * Cache for configurations of filters.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final FiltersCache filtersCache;

    /**
     * Ctor with all fake initialized caches.
     */
    public TestArtipieCaches() {
        this.strgcache = new TestStoragesCache();
        this.cleanuser = new AtomicLong();
        this.cleanpolicy = new AtomicLong();
        this.filtersCache = new TestFiltersCache();
    }

    @Override
    public StoragesCache storagesCache() {
        return this.strgcache;
    }

    @Override
    public Cleanable<String> usersCache() {
        return new Cleanable<>() {
            @Override
            public void invalidate(final String uname) {
                TestArtipieCaches.this.cleanuser.incrementAndGet();
            }

            @Override
            public void invalidateAll() {
                throw new NotImplementedException("method not implemented");
            }
        };
    }

    @Override
    public Cleanable<String> policyCache() {
        return new Cleanable<>() {
            @Override
            public void invalidate(final String uname) {
                TestArtipieCaches.this.cleanpolicy.incrementAndGet();
            }

            @Override
            public void invalidateAll() {
                throw new NotImplementedException("not implemented");
            }
        };
    }

    @Override
    public FiltersCache filtersCache() {
        return this.filtersCache;
    }

    /**
     * True if invalidate method of the {@link Cleanable} for users was called exactly one time.
     * @return True if invalidated
     */
    public boolean wereUsersInvalidated() {
        return this.cleanuser.get() == 1;
    }

    /**
     * True if invalidate method of the {@link Cleanable} for policy was called exactly one time.
     * @return True if invalidated
     */
    public boolean wasPolicyInvalidated() {
        return this.cleanpolicy.get() == 1;
    }
}
