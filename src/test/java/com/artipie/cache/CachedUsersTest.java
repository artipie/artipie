/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.artipie.http.auth.Authentication;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
     * Cache for users.
     */
    private Cache<CachedUsers.Data, Optional<Authentication.User>> cache;

    @BeforeEach
    void setUp() {
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
            .softValues().build();
    }

    @Test
    void callsOriginOnlyOnce() {
        final String username = "usr";
        final AtomicInteger counter = new AtomicInteger();
        final String expected = "usr-1";
        final CachedUsers target = new CachedUsers(this.cache);
        MatcherAssert.assertThat(
            "Wrong user on first cache call",
            target.user(
                username,
                "",
                (usr, pass) -> Optional.of(
                    new Authentication.User(String.format("%s-%d", usr, counter.incrementAndGet()))
                )
            ).orElseThrow().name(),
            new IsEqual<>(expected)
        );
        MatcherAssert.assertThat(
            "Wrong user on second cache call",
            target.user(
                username,
                "",
                (usr, pass) -> Optional.of(
                    new Authentication.User(String.format("%s-%d", usr, counter.incrementAndGet()))
                )
            ).orElseThrow().name(),
            new IsEqual<>(expected)
        );
        MatcherAssert.assertThat(
            "More than one time was cached",
            this.cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void failsToGetUserWhenCacheContainsAnotherUser() {
        final String absent = "absent_user";
        final String cached = "cached_user";
        final CachedUsers target = new CachedUsers(this.cache);
        target.user(
            cached, "", (usr, pass) -> Optional.of(new Authentication.User(cached))
        );
        MatcherAssert.assertThat(
            target.user(absent, "", (usr, pass) -> Optional.empty()).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void getsUserFromCache() {
        final String user = "super_user";
        final CachedUsers target = new CachedUsers(this.cache);
        target.user(
            user, "", (usr, pass) -> Optional.of(new Authentication.User(user))
        );
        MatcherAssert.assertThat(
            target.user(user, "", (usr, pass) -> Optional.empty()).isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Size of cache differed from 1",
            this.cache.size(),
            new IsEqual<>(1L)
        );
    }
}
