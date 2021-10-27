/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.ArtipieException;
import com.artipie.ArtipieProperties;
import com.artipie.http.auth.Authentication;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
public final class CachedUsers implements AuthCache {
    /**
     * Cache for users.
     */
    private static LoadingCache<Data, Optional<Authentication.User>> users;

    static {
        final int timeout;
        try {
            timeout = Integer.parseInt(
                Optional.ofNullable(
                    Optional.ofNullable(
                        System.getProperty(ArtipieProperties.AUTH_TIMEOUT)
                    ).orElse(new ArtipieProperties().cachedAuthTimeout())
                ).orElse("300000")
            );
        } catch (final NumberFormatException exc) {
            throw new ArtipieException(
                String.format("Failed to read property '%s'", ArtipieProperties.AUTH_TIMEOUT),
                exc
            );
        }
        CachedUsers.users = CacheBuilder.newBuilder()
            .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Optional<Authentication.User> load(final Data data) {
                        return data.user();
                    }
                }
            );
    }

    @Override
    public Optional<Authentication.User> user(
        final String username,
        final String password,
        final Authentication origin
    ) {
        return CachedUsers.users.getUnchecked(
            new Data(username, password, origin)
        );
    }

    @Override
    public void invalidateAll() {
        CachedUsers.users.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), CachedUsers.users.size()
        );
    }

    /**
     * Extra class for using instance field in static section.
     * @since 0.22
     */
    private static class Data {
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
