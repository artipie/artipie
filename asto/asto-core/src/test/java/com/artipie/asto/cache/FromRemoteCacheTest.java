/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Test for {@link FromRemoteCache}.
 */
final class FromRemoteCacheTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Test cache.
     */
    private Cache cache;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.cache = new FromRemoteCache(this.storage);
    }

    @Test
    void obtainsItemFromRemoteAndCaches() {
        final byte[] content = "123".getBytes();
        final Key key = new Key.From("item");
        Assertions.assertArrayEquals(
            content,
            this.cache.load(
                key,
                () -> CompletableFuture.completedFuture(Optional.of(new Content.From(content))),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            "Returns content from remote"
        );
        Assertions.assertArrayEquals(content, this.storage.value(key).join().asBytes());
    }

    @Test
    void obtainsItemFromCacheIfRemoteValueIsAbsent() {
        final byte[] content = "765".getBytes();
        final Key key = new Key.From("key");
        this.storage.save(key, new Content.From(content)).join();
        Assertions.assertArrayEquals(
            content,
            this.cache.load(
                key,
                () -> CompletableFuture.completedFuture(Optional.empty()),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            "Returns content from cache"
        );
    }

    @Test
    void loadsFromCacheWhenObtainFromRemoteFailed() {
        final byte[] content = "098".getBytes();
        final Key key = new Key.From("some");
        this.storage.save(key, new Content.From(content)).join();
        Assertions.assertArrayEquals(
            content,
            this.cache.load(
                key,
                new Remote.Failed(new IOException("IO error")),
                CacheControl.Standard.ALWAYS
            ).toCompletableFuture().join().orElseThrow().asBytes(),
            "Returns content from storage"
        );
    }

    @Test
    void failsIfRemoteNotAvailableAndItemIsNotValid() {
        final Key key = new Key.From("any");
        this.storage.save(key, Content.EMPTY).join();
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> this.cache.load(
                    key,
                    new Remote.Failed(new ConnectException("Not available")),
                    CacheControl.Standard.NO_CACHE
                ).toCompletableFuture().join()
            ).getCause(),
            new IsInstanceOf(ConnectException.class)
        );
    }

}
