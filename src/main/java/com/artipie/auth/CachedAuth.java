/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cached authentication decorator.
 * <p>
 * It remembers the result of decorated authentication provider and returns it
 * instead of calling origin authentication.
 * </p>
 * @since 0.10
 * @todo #285:30min Specify expiration time configuration.
 *  Instead of using scheduled executor to clean-up all cache map, use
 *  some configuration to clean-up only expired items, e.g. if token was not accessed for
 *  X minutes, then remove only this token. Consider using Guava's time-evicted-cache
 *  implementation: https://github.com/google/guava/wiki/CachesExplained#eviction
 * @todo #442:30min Refactor this class to move cache field to instance field
 *  and make sure the instance is not created on each request. Right now we're using
 *  a hotfix - CachedAuth created from settings on each request but uses static hash map
 *  field for caching.
 */
public final class CachedAuth implements Authentication {

    /**
     * Static cache hash map.
     */
    private static final ConcurrentMap<String, Optional<Authentication.User>> CACHE =
        new ConcurrentHashMap<>();

    static {
        Executors.newSingleThreadScheduledExecutor()
            // @checkstyle MagicNumberCheck (1 line)
            .schedule(CachedAuth.CACHE::clear, 5, TimeUnit.MINUTES);
    }

    /**
     * Decorated auth provider.
     */
    private final Authentication origin;

    /**
     * Primary constructor.
     * @param origin Origin auth provider
     */
    public CachedAuth(final Authentication origin) {
        this.origin = origin;
    }

    @Override
    public Optional<Authentication.User> user(final String username, final String password) {
        return CachedAuth.CACHE.computeIfAbsent(username, key -> this.origin.user(key, password));
    }

    @Override
    public String toString() {
        return String.format(
            "%s(origin=%s, size=%d)",
            this.getClass().getSimpleName(), this.origin, CachedAuth.CACHE.size()
        );
    }
}
