/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.http.filter.Filters;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of cache for filters using {@link LoadingCache}.
 *
 * @since 0.28
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
public class GuavaFiltersCache implements FiltersCache {
    /**
     * Cache for filters.
     */
    private final Cache<String, Optional<Filters>> cache;

    /**
     * Ctor.
     */
    public GuavaFiltersCache() {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterAccess(
                //@checkstyle MagicNumberCheck (1 line)
                new Property(ArtipieProperties.FILTERS_TIMEOUT).asLongOrDefault(180_000L),
                TimeUnit.MILLISECONDS
            ).softValues()
            .build();
    }

    @Override
    public Optional<Filters> filters(final String reponame,
        final YamlMapping repoyaml) {
        try {
            return this.cache.get(
                reponame,
                () -> Optional.ofNullable(repoyaml.yamlMapping("filters")).map(Filters::new)
            );
        } catch (final ExecutionException err) {
            throw new ArtipieException(err);
        }
    }

    @Override
    public long size() {
        return this.cache.size();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.cache.size()
        );
    }

    @Override
    public void invalidate(final String reponame) {
        this.cache.invalidate(reponame);
    }

    @Override
    public void invalidateAll() {
        this.cache.invalidateAll();
    }
}
