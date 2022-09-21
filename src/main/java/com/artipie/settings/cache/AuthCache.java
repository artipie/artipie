/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.http.auth.Authentication;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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

        /**
         * Counter for `invalidateAll()` method calls.
         */
        private final AtomicInteger cnt;

        /**
         * Ctor.
         */
        public Fake() {
            this.cnt = new AtomicInteger(0);
        }

        @Override
        public Optional<Authentication.User> user(
            final String username,
            final String password,
            final Authentication origin
        ) {
            return origin.user(username, password);
        }

        @Override
        public void invalidateAll() {
            this.cnt.incrementAndGet();
        }

        /**
         * Was `invalidateAll()` called?
         * @return True if `invalidateAll()` was called exactly once
         */
        public boolean wasInvalidated() {
            return this.cnt.get() == 1;
        }
    }
}
