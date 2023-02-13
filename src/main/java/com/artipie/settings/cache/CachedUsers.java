/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.asto.misc.UncheckedScalar;
import com.artipie.http.auth.Authentication;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Cached authentication decorator.
 * <p>
 * It remembers the result of decorated authentication provider and returns it
 * instead of calling origin authentication.
 * </p>
 * @since 0.22
 */
public final class CachedUsers implements Authentication, Cleanable {
    /**
     * Cache for users. The key is md5 calculated from username and password
     * joined with space.
     */
    private final Cache<String, Optional<Authentication.User>> users;

    /**
     * Origin authentication.
     */
    private final Authentication origin;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     * @param origin Origin authentication
     */
    public CachedUsers(final Authentication origin) {
        this(
            origin,
            CacheBuilder.newBuilder()
                .expireAfterAccess(
                    //@checkstyle MagicNumberCheck (1 line)
                    new Property(ArtipieProperties.AUTH_TIMEOUT).asLongOrDefault(300_000L),
                    TimeUnit.MILLISECONDS
                ).softValues()
                .build()
        );
    }

    /**
     * Ctor.
     * @param origin Origin authentication
     * @param cache Cache for users
     */
    CachedUsers(
        final Authentication origin,
        final Cache<String, Optional<Authentication.User>> cache
    ) {
        this.users = cache;
        this.origin = origin;
    }

    @Override
    public Optional<Authentication.User> user(
        final String username,
        final String password
    ) {
        final String key = DigestUtils.md5Hex(String.join(" ", username, password));
        return new UncheckedScalar<>(
            () -> this.users.get(key, () -> this.origin.user(username, password))
        ).value();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d),origin=%s",
            this.getClass().getSimpleName(), this.users.size(),
            this.origin.toString()
        );
    }

    @Override
    public void invalidate() {
        this.users.invalidateAll();
    }
}
