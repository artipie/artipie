/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.repo.proxy;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.time.Duration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link YamlProxyStorage}.
 * @since 0.23
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class YamlCacheStorageTest {
    @Test
    void returnsDefaultMaxSizeWhenItIsAbsentInConfig() {
        MatcherAssert.assertThat(
            new YamlProxyStorage(new InMemoryStorage()).maxSize(),
            new IsEqual<>(Long.MAX_VALUE)
        );
    }

    @Test
    void returnsDefaultTtlWhenItIsAbsentInConfig() {
        MatcherAssert.assertThat(
            new YamlProxyStorage(new InMemoryStorage()).timeToLive(),
            new IsEqual<>(Duration.ofMillis(Long.MAX_VALUE))
        );
    }

    @Test
    void returnsProvidedTtl() {
        final Duration ttl = Duration.ofHours(1);
        MatcherAssert.assertThat(
            new YamlProxyStorage(new InMemoryStorage(), 10L, ttl).timeToLive(),
            new IsEqual<>(ttl)
        );
    }

    @Test
    void returnsProvidedMaxSize() {
        final long size = 1001L;
        MatcherAssert.assertThat(
            new YamlProxyStorage(new InMemoryStorage(), size, Duration.ZERO).maxSize(),
            new IsEqual<>(size)
        );
    }

    @Test
    void returnsProvidedStorage() {
        final Storage asto = new InMemoryStorage();
        MatcherAssert.assertThat(
            new YamlProxyStorage(asto, 100L, Duration.ZERO).storage(),
            new IsEqual<>(asto)
        );
    }
}
