/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CachedCreds}.
 * @since 0.23
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CachedCredsTest {
    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Cache for storages settings.
     */
    private Cache<CachedCreds.Metadata, CompletionStage<YamlMapping>> cache;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
            .softValues().build();
    }

    @Test
    void getsValueFromCache() {
        final Key path = new Key.From("creds.yaml");
        final CredsConfigCache configs = new CachedCreds(this.cache);
        configs.invalidateAll();
        this.storage.save(path, Content.EMPTY).join();
        final YamlMapping creds = configs.credentials(this.storage, path)
            .toCompletableFuture().join();
        final YamlMapping same = configs.credentials(this.storage, path)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were different",
            creds.equals(same),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            this.cache.size(),
            new IsEqual<>(1L)
        );
    }

    @Test
    void getsOriginForDifferentConfigurations() {
        final CredsConfigCache configs = new CachedCreds(this.cache);
        configs.invalidateAll();
        final Key onekey = new Key.From("first.yml");
        final Key twokey = new Key.From("credentials.yml");
        final BlockingStorage blck = new BlockingStorage(this.storage);
        blck.save(onekey, "credentials: val".getBytes(StandardCharsets.UTF_8));
        blck.save(twokey, "credentials: another val".getBytes(StandardCharsets.UTF_8));
        final YamlMapping frst = configs.credentials(this.storage, onekey)
            .toCompletableFuture().join();
        final YamlMapping scnd = configs.credentials(this.storage, twokey)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            this.cache.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void getsOriginForSameConfigurationButDifferentStorages() {
        final String path = "_credentials.yaml";
        final Key key = new Key.From(path);
        final CredsConfigCache configs = new CachedCreds(this.cache);
        configs.invalidateAll();
        final Storage another = new InMemoryStorage();
        new TestResource(path).saveTo(this.storage);
        new BlockingStorage(another)
            .save(key, "credentials: another val".getBytes(StandardCharsets.UTF_8));
        final YamlMapping frst = configs.credentials(this.storage, key)
            .toCompletableFuture().join();
        final YamlMapping scnd = configs.credentials(another, key)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            this.cache.size(),
            new IsEqual<>(2L)
        );
    }
}
