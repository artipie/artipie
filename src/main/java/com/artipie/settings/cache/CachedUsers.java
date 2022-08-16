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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cached authentication decorator.
 * <p>
 * It remembers the result of decorated authentication provider and returns it
 * instead of calling origin authentication.
 * </p>
 * @since 0.22
 */
final class CachedUsers implements AuthCache {
    /**
     * Cache for users.
     */
    private final Cache<Data, Optional<Authentication.User>> users;

    /**
     * Ctor.
     * Here an instance of cache is created. It is important that cache
     * is a local variable.
     */
    CachedUsers() {
        this(
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
     * @param cache Cache for users
     */
    CachedUsers(final Cache<Data, Optional<Authentication.User>> cache) {
        this.users = cache;
    }

    @Override
    public Optional<Authentication.User> user(
        final String username,
        final String password,
        final Authentication origin
    ) {
        final Data data = new Data(username, password, origin);
        return new UncheckedScalar<>(
            () -> this.users.get(data, data::user)
        ).value();
    }

    @Override
    public void invalidateAll() {
        this.users.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), this.users.size()
        );
    }

    /**
     * Extra class for using instance field in static section.
     * @since 0.22
     */
    static class Data {
        /**
         * Username.
         */
        private final String username;

        /**
         * Password.
         */
        private final String pswd;

        /**
         * Auth provider.
         */
        private final Authentication origin;

        /**
         * Ctor.
         * @param username Username
         * @param pswd Password
         * @param origin Auth provider
         */
        Data(final String username, final String pswd, final Authentication origin) {
            this.username = username;
            this.pswd = pswd;
            this.origin = origin;
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (this == obj) {
                res = true;
            } else if (obj == null || this.getClass() != obj.getClass()) {
                res = false;
            } else {
                final Data data = (Data) obj;
                res = Objects.equals(this.username, data.username);
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.username);
        }

        /**
         * Find user by credentials.
         * @return User login if found.
         */
        Optional<Authentication.User> user() {
            return this.origin.user(this.username, this.pswd);
        }
    }
}
