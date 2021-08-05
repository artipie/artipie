/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.repo.ConfigFile;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Artipie repositories created from {@link Settings}.
 *
 * @since 0.13
 */
public final class RepositoriesFromStorage implements Repositories {
    /**
     * Cache for config files.
     */
    private static LoadingCache<KeyAndStorage, Single<String>> configs;

    /**
     * Cache for aliases.
     */
    private static LoadingCache<KeyAndStorage, Single<StorageAliases>> aliases;

    static {
        final CacheLoader<KeyAndStorage, Single<String>> ldrconfigs = new CacheLoader<>() {
            @Override
            public Single<String> load(final KeyAndStorage pair) {
                return Single.fromFuture(
                    new ConfigFile(pair.key)
                        .valueFrom(pair.storage)
                        .thenApply(PublisherAs::new)
                        .thenCompose(PublisherAs::asciiString)
                        .toCompletableFuture()
                );
            }
        };
        final CacheLoader<KeyAndStorage, Single<StorageAliases>> ldralias = new CacheLoader<>() {
            @Override
            public Single<StorageAliases> load(final KeyAndStorage pair) {
                return Single.fromFuture(
                    StorageAliases.find(pair.storage, pair.key)
                );
            }
        };
        final int timeout = new ArtipieProperties().configCacheTimeout();
        RepositoriesFromStorage.configs = CacheBuilder.newBuilder()
            .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
            .softValues().build(ldrconfigs);
        RepositoriesFromStorage.aliases = CacheBuilder.newBuilder()
            .expireAfterAccess(timeout, TimeUnit.MILLISECONDS)
            .softValues().build(ldralias);
    }

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage.
     */
    public RepositoriesFromStorage(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<RepoConfig> config(final String name) {
        final KeyAndStorage pair = new KeyAndStorage(new Key.From(name), this.storage);
        return Single.zip(
            RepositoriesFromStorage.configs.getUnchecked(pair),
            RepositoriesFromStorage.aliases.getUnchecked(pair),
            (data, alias) -> SingleInterop.fromFuture(
                RepoConfig.fromPublisher(alias, pair.key, new Content.From(data.getBytes()))
            )
        ).flatMap(self -> self).to(SingleInterop.get());
    }

    /**
     * Extra class for passing pair of values.
     * @since 0.22
     */
    private static final class KeyAndStorage {
        /**
         * Key.
         */
        private final Key key;

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Ctor.
         * @param key Key
         * @param storage Storage
         */
        private KeyAndStorage(final Key key, final Storage storage) {
            this.key = key;
            this.storage = storage;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (obj == this) {
                res = true;
            } else if (obj instanceof KeyAndStorage) {
                final KeyAndStorage data = (KeyAndStorage) obj;
                res = this.key.equals(data.key) && Objects.equals(data.storage, this.storage);
            } else {
                res = false;
            }
            return res;
        }
    }
}
