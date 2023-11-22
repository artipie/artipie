/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.memory.InMemoryStorage;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link CacheTimeControl}.
 * @since 0.4
 */
final class CacheTimeControlTest {
    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @CsvSource({"1,true", "12,false"})
    void verifiesTimeValueCorrectly(final long minutes, final boolean valid) {
        final String pkg = "vendor/package";
        new BlockingStorage(this.storage).save(
            CacheTimeControl.CACHE_FILE,
            Json.createObjectBuilder()
                .add(
                    pkg,
                    ZonedDateTime.ofInstant(
                        Instant.now(),
                        ZoneOffset.UTC
                    ).minusMinutes(minutes).toString()
                ).build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            this.validate(pkg),
            new IsEqual<>(valid)
        );
    }

    @Test
    void falseForAbsentPackageInCacheFile() {
        new BlockingStorage(this.storage).save(
            CacheTimeControl.CACHE_FILE,
            Json.createObjectBuilder().build().toString().getBytes()
        );
        MatcherAssert.assertThat(
            this.validate("not/exist"),
            new IsEqual<>(false)
        );
    }

    @Test
    void falseIfCacheIsAbsent() {
        MatcherAssert.assertThat(
            this.validate("file/notexist"),
            new IsEqual<>(false)
        );
    }

    private boolean validate(final String pkg) {
        return new CacheTimeControl(this.storage)
            .validate(new Key.From(pkg), Remote.EMPTY)
            .toCompletableFuture().join();
    }
}
