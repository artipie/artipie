/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

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
 * @since 0.10
 */
public final class CachedAuth implements AuthCache {
    /**
     * Cache for users.
     */
    private static LoadingCache<Data, Optional<Authentication.User>> users;

    static {
        System.setProperty(
            ArtipieProperties.AUTH_TIMEOUT,
            new ArtipieProperties().cachedAuthTimeout()
        );
        final int timeout = Integer.getInteger(ArtipieProperties.AUTH_TIMEOUT, 5 * 60 * 1000);
        CachedAuth.users = CacheBuilder.newBuilder()
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
        return CachedAuth.users.getUnchecked(
            new Data(username, password, origin)
        );
    }

    @Override
    public void invalidateAll() {
        CachedAuth.users.invalidateAll();
    }

    @Override
    public String toString() {
        return String.format(
            "%s(size=%d)",
            this.getClass().getSimpleName(), CachedAuth.users.size()
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
