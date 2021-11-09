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
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
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

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void getsValueFromCache() {
        final Key path = new Key.From("creds.yaml");
        final CredsConfigCache cache = new CachedCreds();
        this.storage.save(path, Content.EMPTY).join();
        final YamlMapping creds = cache.credentials(this.storage, path)
            .toCompletableFuture().join();
        final YamlMapping same = cache.credentials(this.storage, path)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were different",
            creds.equals(same),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            cache.toString(),
            new StringContains("size=1")
        );
    }

    @Test
    void getsOriginForDifferentConfigurations() {
        final CredsConfigCache cache = new CachedCreds();
        final Key onekey = new Key.From("first.yml");
        final Key twokey = new Key.From("credentials.yml");
        final BlockingStorage blck = new BlockingStorage(this.storage);
        blck.save(onekey, "credentials: val".getBytes(StandardCharsets.UTF_8));
        blck.save(twokey, "credentials: another val".getBytes(StandardCharsets.UTF_8));
        final YamlMapping frst = cache.credentials(this.storage, onekey)
            .toCompletableFuture().join();
        final YamlMapping scnd = cache.credentials(this.storage, twokey)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            cache.toString(),
            new StringContains("size=2")
        );
    }

    @Test
    void getsOriginForSameConfigurationButDifferentStorages() {
        final String path = "_credentials.yaml";
        final Key key = new Key.From(path);
        final CredsConfigCache cache = new CachedCreds();
        final Storage another = new InMemoryStorage();
        new TestResource(path).saveTo(this.storage);
        new BlockingStorage(another)
            .save(key, "credentials: another val".getBytes(StandardCharsets.UTF_8));
        final YamlMapping frst = cache.credentials(this.storage, key)
            .toCompletableFuture().join();
        final YamlMapping scnd = cache.credentials(another, key)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Obtained configurations were the same",
            frst.equals(scnd),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Credentials configuration was not cached",
            cache.toString(),
            new StringContains("size=2")
        );
    }
}
