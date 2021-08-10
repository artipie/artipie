/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Optional;

/**
 * Cache for user logins which were found by credentials.
 * @since 0.22
 */
public interface AuthCache {
    /**
     * Find user by credentials.
     * @param username Username
     * @param password Password
     * @param origin Auth provider
     * @return User login if found
     */
    Optional<Authentication.User> user(String username, String password, Authentication origin);

    /**
     * Invalidate all items in cache.
     */
    void invalidateAll();

    /**
     * Fake implementation of {@link AuthCache}.
     * @since 0.22
     */
    class Fake implements AuthCache {
        @Override
        public Optional<Authentication.User> user(
            final String username,
            final String password,
            final Authentication origin
        ) {
            return Optional.empty();
        }

        @Override
        public void invalidateAll() {
            // do nothing
        }
    }
}
