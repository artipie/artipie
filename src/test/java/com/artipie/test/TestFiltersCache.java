/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import com.artipie.settings.cache.CachedFilters;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test filters caches.
 * @since 0.28
 */
public final class TestFiltersCache extends CachedFilters {

    /**
     * Counter for `invalidateAll()` method calls.
     */
    private final AtomicInteger cnt;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     */
    public TestFiltersCache() {
        super();
        this.cnt = new AtomicInteger(0);
    }

    @Override
    public void invalidateAll() {
        this.cnt.incrementAndGet();
        super.invalidateAll();
    }

    @Override
    public void invalidate(final String reponame) {
        this.cnt.incrementAndGet();
        super.invalidate(reponame);
    }

    /**
     * Was this case invalidated?
     *
     * @return True, if it was invalidated once
     */
    public boolean wasInvalidated() {
        return this.cnt.get() == 1;
    }
}
