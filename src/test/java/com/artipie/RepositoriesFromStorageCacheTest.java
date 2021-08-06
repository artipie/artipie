/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for cache of files with configuration in {@link RepositoriesFromStorage}.
 *
 * @since 0.22
 */
final class RepositoriesFromStorageCacheTest {
    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void readConfigFromCacheAfterSavingNewValueInStorage() {
        final Key key = new Key.From("some-repo.yaml");
        final byte[] old = "some: data".getBytes();
        final byte[] upd = "some: new data".getBytes();
        new BlockingStorage(this.storage).save(key, old);
        new RepositoriesFromStorage(this.storage).config(key.string())
            .toCompletableFuture().join();
        new BlockingStorage(this.storage).save(key, upd);
        MatcherAssert.assertThat(
            new RepositoriesFromStorage(this.storage).config(key.string())
                .toCompletableFuture().join()
                .toString(),
            new IsEqual<>(new String(old))
        );
    }

    @Test
    void readAliasesFromCache() {
        final Key alias = new Key.From("_storages.yaml");
        final Key config = new Key.From("bin.yaml");
        new TestResource(alias.string()).saveTo(this.storage);
        new BlockingStorage(this.storage).save(config, "repo:\n  storage: default".getBytes());
        new RepositoriesFromStorage(this.storage).config(config.string())
            .toCompletableFuture().join();
        this.storage.save(alias, Content.EMPTY).join();
        MatcherAssert.assertThat(
            new RepositoriesFromStorage(this.storage).config(config.string())
                .toCompletableFuture().join()
                .storageOpt().isPresent(),
            new IsEqual<>(true)
        );
    }
}
