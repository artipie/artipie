/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.artipie.http.auth.Authentication;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link CachedAuth}.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedAuthTest {
    /**
     * Cached authentication.
     */
    private CachedAuth target;

    @BeforeEach
    void setUp() {
        this.target = new CachedAuth();
        this.target.invalidateAll();
    }

    @Test
    void callsOriginOnlyOnce() {
        final String username = "usr";
        final AtomicInteger counter = new AtomicInteger();
        final String expected = "usr-1";
        MatcherAssert.assertThat(
            "Wrong user on first cache call",
            this.target.user(
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
            this.target.user(
                username,
                "",
                (usr, pass) -> Optional.of(
                    new Authentication.User(String.format("%s-%d", usr, counter.incrementAndGet()))
                )
            ).orElseThrow().name(),
            new IsEqual<>(expected)
        );
    }

    @Test
    void failsToGetUserWhenCacheContainsAnotherUser() {
        final String absent = "absent_user";
        final String cached = "cached_user";
        this.target.user(
            cached, "", (usr, pass) -> Optional.of(new Authentication.User(cached))
        );
        MatcherAssert.assertThat(
            this.target.user(absent, "", (usr, pass) -> Optional.empty()).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void getsUserFromCache() {
        final String user = "super_user";
        this.target.user(
            user, "", (usr, pass) -> Optional.of(new Authentication.User(user))
        );
        MatcherAssert.assertThat(
            this.target.user(user, "", (usr, pass) -> Optional.empty()).isPresent(),
            new IsEqual<>(true)
        );
    }
}
