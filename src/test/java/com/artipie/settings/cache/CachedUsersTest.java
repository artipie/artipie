/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.cache;

import com.artipie.http.auth.Authentication;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link CachedUsers}.
 *
 * @since 0.22
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedUsersTest {

    /**
     * Test cache.
     */
    private Cache<String, Optional<Authentication.User>> cache;

    /**
     * Test users.
     */
    private CachedUsers users;

    /**
     * Test authentication.
     */
    private FakeAuth auth;

    @BeforeEach
    void init() {
        this.cache = CacheBuilder.newBuilder().build();
        this.auth = new FakeAuth();
        this.users = new CachedUsers(this.auth, this.cache);
    }

    @Test
    void authenticatesAndCachesResult() {
        MatcherAssert.assertThat(
            "Jane was authenticated on the first call",
            this.users.user("jane", "any").isPresent()
        );
        MatcherAssert.assertThat(
            "Cache size should be 1",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Jane was authenticated on the second call",
            this.users.user("jane", "any").isPresent()
        );
        MatcherAssert.assertThat(
            "Cache size should be 1",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Authenticate method should be called only once",
            this.auth.cnt.get(),
            new IsEqual<>(1)
        );
    }

    @Test
    void cachesWhenNotAuthenticated() {
        MatcherAssert.assertThat(
            "David was not authenticated on the first call",
            this.users.user("David", "any").isEmpty()
        );
        MatcherAssert.assertThat(
            "olga was not authenticated on the first call",
            this.users.user("Olga", "any").isEmpty()
        );
        MatcherAssert.assertThat(
            "Cache size should be 2",
            this.cache.size(),
            new IsEqual<>(2L)
        );
        MatcherAssert.assertThat(
            "David was not authenticated on the second call",
            this.users.user("David", "any").isEmpty()
        );
        MatcherAssert.assertThat(
            "olga was not authenticated on the second call",
            this.users.user("Olga", "any").isEmpty()
        );
        MatcherAssert.assertThat(
            "Cache size should be 2",
            this.cache.size(),
            new IsEqual<>(2L)
        );
        MatcherAssert.assertThat(
            "Authenticate method should be called twice",
            this.auth.cnt.get(),
            new IsEqual<>(2)
        );
    }

    /**
     * Fake authentication: returns "jane" when username is jane, empty otherwise.
     * @since 0.27
     */
    final class FakeAuth implements Authentication {

        /**
         * Method call count.
         */
        private final AtomicInteger cnt = new AtomicInteger();

        @Override
        public Optional<User> user(final String name, final String pswd) {
            this.cnt.incrementAndGet();
            final Optional<User> res;
            if (name.equals("jane")) {
                res = Optional.of(new Authentication.User(name));
            } else {
                res = Optional.empty();
            }
            return res;
        }
    }

}
