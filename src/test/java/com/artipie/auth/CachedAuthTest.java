/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.auth;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
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
    void usesCache() {
        final String username = "test/user";
        final String user = "user";
        MatcherAssert.assertThat(
            new CachedAuth(
                (usr, pass) -> Optional.empty(),
                new ConcurrentHashMap<>(new MapOf<>(new MapEntry<>(username, Optional.of(user))))
            ).user(username, "any").orElseThrow(),
            new IsEqual<>(user)
        );
    }

    @Test
    void callsOriginOnlyOnce() {
        final String username = "usr";
        final AtomicInteger counter = new AtomicInteger();
        final CachedAuth target = new CachedAuth(
            (usr, pass) -> Optional.of(String.format("%s-%d", usr, counter.incrementAndGet())),
            new ConcurrentHashMap<>()
        );
        final String expected = "usr-1";
        MatcherAssert.assertThat(
            "Wrong user on first cache call",
            target.user(username, "").orElseThrow(),
            new IsEqual<>(expected)
        );
        MatcherAssert.assertThat(
            "Wrong user on second cache call",
            target.user(username, "").orElseThrow(),
            new IsEqual<>(expected)
        );
    }
}
