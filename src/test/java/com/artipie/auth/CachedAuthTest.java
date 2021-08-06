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
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link CachedAuth}.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedAuthTest {
    @Test
    void callsOriginOnlyOnce() {
        final String username = "usr";
        final AtomicInteger counter = new AtomicInteger();
        final String expected = "usr-1";
        final CachedAuth target = new CachedAuth();
        target.invalidateAll();
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
    }

    @Test
    void failsToGetUserWhenCacheContainsAnotherUser() {
        final String absent = "absent_user";
        final String cached = "cached_user";
        final CachedAuth target = new CachedAuth();
        target.invalidateAll();
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
        final CachedAuth target = new CachedAuth();
        target.invalidateAll();
        target.user(
            user, "", (usr, pass) -> Optional.of(new Authentication.User(user))
        );
        MatcherAssert.assertThat(
            target.user(user, "", (usr, pass) -> Optional.empty()).isPresent(),
            new IsEqual<>(true)
        );
    }
}
