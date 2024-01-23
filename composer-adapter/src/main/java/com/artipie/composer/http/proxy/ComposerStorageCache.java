/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.Repository;
import com.artipie.composer.misc.ContentAsJson;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Cache implementation that tries to obtain items from storage cache,
 * validates it and returns if valid. If item is not present in storage or is not valid,
 * it is loaded from remote.
 * @since 0.4
 */
public final class ComposerStorageCache implements Cache {
    /**
     * Folder for cached items.
     */
    static final String CACHE_FOLDER = "cache";

    /**
     * Repository.
     */
    private final Repository repo;

    /**
     * Ctor.
     * @param repository Repository
     */
    public ComposerStorageCache(final Repository repository) {
        this.repo = repository;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(
        final Key name, final Remote remote, final CacheControl control
    ) {
        final Key cached = new Key.From(
            ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", name.string())
        );
        return this.repo.exists(cached)
            .thenCompose(
                exists -> {
                    final CompletionStage<Optional<? extends Content>> res;
                    if (exists) {
                        res = control.validate(
                            name,
                            () -> CompletableFuture.completedFuture(Optional.empty())
                        ).thenCompose(
                            valid -> {
                                final CompletionStage<Optional<Content>> cacheval;
                                if (valid) {
                                    cacheval = this.repo.value(cached).thenApply(Optional::of);
                                } else {
                                    cacheval = CompletableFuture.completedFuture(Optional.empty());
                                }
                                return cacheval;
                            }
                        ).thenCompose(
                            cacheres -> {
                                final CompletionStage<Optional<? extends Content>> rmtorcache;
                                if (cacheres.isPresent()) {
                                    rmtorcache = CompletableFuture.completedFuture(cacheres);
                                } else {
                                    rmtorcache = this.contentFromRemote(remote, cached, name);
                                }
                                return rmtorcache;
                            }
                        );
                    } else {
                        res = this.contentFromRemote(remote, cached, name);
                    }
                    return res;
                }
            );
    }

    /**
     * Obtains and caches content from remote in case of existence, empty otherwise.
     * @param remote Remote content
     * @param cached Key for obtaining cached package
     * @param name Name of cached item (usually like `vendor/package`)
     * @return Content from remote if exists, empty otherwise.
     */
    private CompletableFuture<Optional<? extends Content>> contentFromRemote(
        final Remote remote, final Key cached, final Key name
    ) {
        return CompletableFuture.supplyAsync(() -> null)
            .thenCombine(
                remote.get(),
                (nothing, content) -> {
                    final CompletionStage<Optional<? extends Content>> res;
                    if (content.isPresent()) {
                        res = this.repo.save(cached, content.get())
                            .thenCompose(noth -> this.updateCacheFile(cached, name))
                            .thenCompose(ignore -> this.repo.value(cached))
                            .thenApply(Optional::of);
                    } else {
                        res = CompletableFuture.completedFuture(Optional.empty());
                    }
                    return res;
                }
            ).thenCompose(Function.identity());
    }

    /**
     * Update existed in storage cache file.
     * @param cached Key for obtaining cached package
     * @param name Name of cached item (usually like `vendor/package`)
     * @return Result of completion.
     */
    private CompletionStage<Void> updateCacheFile(final Key cached, final Key name) {
        final Key tmp = new Key.From(
            String.format("%s%s.json", cached, UUID.randomUUID().toString())
        );
        return this.repo.exists(CacheTimeControl.CACHE_FILE)
            .thenCompose(this::createCacheFileIfAbsent)
            .thenCompose(
                nothing -> this.repo.exclusively(
                    CacheTimeControl.CACHE_FILE,
                    nthng -> this.repo.value(CacheTimeControl.CACHE_FILE)
                        .thenApply(ContentAsJson::new)
                        .thenCompose(ContentAsJson::value)
                        .thenApply(json -> ComposerStorageCache.addTimeFor(json, name))
                        .thenCompose(json -> this.repo.save(tmp, new Content.From(json)))
                        .thenCompose(noth -> this.repo.delete(CacheTimeControl.CACHE_FILE))
                        .thenCompose(noth -> this.repo.move(tmp, CacheTimeControl.CACHE_FILE))
                )
            );
    }

    /**
     * Creates cache file in case of absent.
     * @param exists Does file exists?
     * @return Result of completion.
     */
    private CompletionStage<Void> createCacheFileIfAbsent(final boolean exists) {
        final CompletionStage<Void> res;
        if (exists) {
            res = CompletableFuture.allOf();
        } else {
            res = this.repo.save(
                CacheTimeControl.CACHE_FILE,
                new Content.From(
                    Json.createObjectBuilder().build()
                        .toString().getBytes()
                )
            );
        }
        return res;
    }

    /**
     * Add time in json for passed item.
     * @param json JSON file (e.g. which contains info for cached items)
     * @param name Item which should be added
     * @return Updated JSON with added info for passed key.
     */
    private static byte[] addTimeFor(final JsonObject json, final Key name) {
        return Json.createObjectBuilder(json)
            .add(
                name.string(),
                ZonedDateTime.ofInstant(
                    Instant.now(),
                    ZoneOffset.UTC
                ).toString()
            ).build().toString()
            .getBytes();
    }
}
