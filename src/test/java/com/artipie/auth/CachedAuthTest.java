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
final class CachedAuthTest {

    @Test
    void callsOriginOnlyOnce() {
        final String username = "usr";
        final AtomicInteger counter = new AtomicInteger();
        final CachedAuth target = new CachedAuth(
            (usr, pass) -> Optional.of(
                new Authentication.User(String.format("%s-%d", usr, counter.incrementAndGet()))
            )
        );
        final String expected = "usr-1";
        MatcherAssert.assertThat(
            "Wrong user on first cache call",
            target.user(username, "").orElseThrow().name(),
            new IsEqual<>(expected)
        );
        MatcherAssert.assertThat(
            "Wrong user on second cache call",
            target.user(username, "").orElseThrow().name(),
            new IsEqual<>(expected)
        );
    }
}
