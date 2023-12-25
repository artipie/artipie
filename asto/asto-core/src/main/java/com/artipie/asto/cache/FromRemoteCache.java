/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * This cache implementation loads all the items from remote and caches it to storage. Content
 * is loaded from cache only if remote failed to return requested item.
 * @since 0.30
 */
public final class FromRemoteCache implements Cache {

    /**
     * Back-end storage.
     */
    private final Storage storage;

    /**
     * New remote cache.
     * @param storage Back-end storage for cache
     */
    public FromRemoteCache(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(
        final Key key, final Remote remote, final CacheControl control
    ) {
        return remote.get().handle(
            (content, throwable) -> {
                final CompletionStage<Optional<? extends Content>> res;
                if (throwable == null && content.isPresent()) {
                    res = this.storage.save(
                        key, new Content.From(content.get().size(), content.get())
                    ).thenCompose(nothing -> this.storage.value(key))
                        .thenApply(Optional::of);
                } else {
                    final Throwable error;
                    if (throwable == null) {
                        error = new ArtipieIOException("Failed to load content from remote");
                    } else {
                        error = throwable;
                    }
                    res = new FromStorageCache(this.storage)
                        .load(key, new Remote.Failed(error), control);
                }
                return res;
            }
        ).thenCompose(Function.identity());
    }
}
