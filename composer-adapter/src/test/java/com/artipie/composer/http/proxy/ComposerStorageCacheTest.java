/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.Repository;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link ComposerStorageCache}.
 */
final class ComposerStorageCacheTest {

    private Storage storage;

    private Repository repo;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.repo = new AstoRepository(this.storage);
    }

    @Test
    void getsContentFromRemoteCachesItAndSaveKeyToCacheFile() {
        final byte[] body = "some info".getBytes();
        final String key = "vendor/package";
        MatcherAssert.assertThat(
            "Content was not obtained from remote",
            new ComposerStorageCache(this.repo).load(
                new Key.From(key),
                () -> CompletableFuture.completedFuture(Optional.of(new Content.From(body))),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat(
            "Item was not cached",
            this.storage.exists(
                new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key))
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Info about save time was not saved in cache file",
            this.storage.value(CacheTimeControl.CACHE_FILE).join()
                .asJsonObject().keySet(),
            new IsEqual<>(new SetOf<>(key))
        );
    }

    @Test
    void getsContentFromCache() {
        final byte[] body = "some info".getBytes();
        final String key = "vendor/package";
        this.saveCacheFile(key);
        this.storage.save(
            new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key)),
            new Content.From(body)
        ).join();
        MatcherAssert.assertThat(
            new ComposerStorageCache(this.repo).load(
                new Key.From(key),
                () -> CompletableFuture.completedFuture(Optional.empty()),
                new CacheTimeControl(this.storage)
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            new IsEqual<>(body)
        );
    }

    @Test
    void getsContentFromRemoteForExpiredCacheAndOverwriteValues() {
        final byte[] body = "some info".getBytes();
        final byte[] updated = "updated some info".getBytes();
        final String key = "vendor/package";
        final String expired = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            .minus(Duration.ofDays(100))
            .toString();
        this.saveCacheFile(key, expired);
        this.storage.save(
            new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key)),
            new Content.From(body)
        ).join();
        MatcherAssert.assertThat(
            "Content was not obtained from remote when cache is expired",
            new ComposerStorageCache(this.repo).load(
                new Key.From(key),
                () -> CompletableFuture.completedFuture(Optional.of(new Content.From(updated))),
                new CacheTimeControl(this.storage)
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            new IsEqual<>(updated)
        );
        MatcherAssert.assertThat(
            "Info about save time was not updated in cache file",
            this.storage.value(CacheTimeControl.CACHE_FILE).join()
                .asJsonObject().getString(key),
            new IsNot<>(new IsEqual<>(expired))
        );
        MatcherAssert.assertThat(
            "Cached item was not overwritten",
            this.storage.value(
                new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", key))
            ).join().asBytes(),
            new IsEqual<>(updated)
        );
    }

    @Test
    void returnsEmptyOnRemoteErrorAndEmptyCache() {
        MatcherAssert.assertThat(
            "Was not empty for remote error and empty cache",
            new ComposerStorageCache(this.repo).load(
                new Key.From("anykey"),
                new Remote.WithErrorHandling(
                    () -> new FailedCompletionStage<>(
                        new IllegalStateException("Failed to obtain item from cache")
                    )
                ),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join()
            .isPresent(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Cache storage is not empty",
            this.storage.list(Key.ROOT)
                .join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    private void saveCacheFile(final String key) {
        this.saveCacheFile(
            key,
            ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toString()
        );
    }

    private void saveCacheFile(final String key, final String expiration) {
        this.storage.save(
            CacheTimeControl.CACHE_FILE,
            new Content.From(
                Json.createObjectBuilder().add(key, expiration)
                    .build().toString()
                    .getBytes()
            )
        ).join();
    }
}
