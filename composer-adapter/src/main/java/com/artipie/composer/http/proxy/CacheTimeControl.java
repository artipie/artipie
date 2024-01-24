/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.CacheControl;
import com.artipie.asto.cache.Remote;
import com.artipie.composer.misc.ContentAsJson;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Check if saved item is expired by comparing time value.
 * @since 0.4
 * @todo #77:30min Move this class to asto.
 *  Move this class to asto module as soon as the implementation will
 *  be checked on convenience and rightness (e.g. this class will be used
 *  for implementation in this adapter and proper tests will be added).
 */
final class CacheTimeControl implements CacheControl {
    /**
     * Name to file which contains info about cached items (e.g. when an item was saved).
     */
    static final Key CACHE_FILE = new Key.From("cache/cache-info.json");

    /**
     * Time during which the file is valid.
     */
    private final Duration expiration;

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor with default value for time of expiration.
     * @param storage Storage
     */
    CacheTimeControl(final Storage storage) {
        this(storage, Duration.ofMinutes(10));
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param expiration Time after which cached items are not valid
     */
    CacheTimeControl(final Storage storage, final Duration expiration) {
        this.storage = storage;
        this.expiration = expiration;
    }

    @Override
    public CompletionStage<Boolean> validate(final Key item, final Remote content) {
        return this.storage.exists(CacheTimeControl.CACHE_FILE)
            .thenCompose(
                exists -> {
                    final CompletionStage<Boolean> res;
                    if (exists) {
                        res = this.storage.value(CacheTimeControl.CACHE_FILE)
                            .thenApply(ContentAsJson::new)
                            .thenCompose(ContentAsJson::value)
                            .thenApply(
                                json -> {
                                    final String key = item.string();
                                    return json.containsKey(key)
                                        && this.notExpired(json.getString(key));
                                }
                            );
                    } else {
                        res = CompletableFuture.completedFuture(false);
                    }
                    return res;
                }
            );
    }

    /**
     * Validate time by comparing difference with time of expiration.
     * @param time Time of uploading
     * @return True is valid as not expired yet, false otherwise.
     */
    private boolean notExpired(final String time) {
        return !Duration.between(
            Instant.now().atZone(ZoneOffset.UTC),
            ZonedDateTime.parse(time)
        ).plus(this.expiration)
        .isNegative();
    }
}
