/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.cache;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Cache implementation that tries to obtain items from storage cache,
 * validates it and returns if valid. If item is not present in storage or is not valid,
 * it is loaded from remote.
 * @since 0.24
 */
public final class FromStorageCache implements Cache {

    /**
     * Back-end storage.
     */
    private final Storage storage;

    /**
     * New storage cache.
     * @param storage Back-end storage for cache
     */
    public FromStorageCache(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Optional<? extends Content>> load(final Key key, final Remote remote,
        final CacheControl control) {
        final RxStorageWrapper rxsto = new RxStorageWrapper(this.storage);
        return rxsto.exists(key)
            .filter(exists -> exists)
            .flatMapSingleElement(
                exists -> SingleInterop.fromFuture(
                    control.validate(key, () -> this.storage.value(key).thenApply(Optional::of))
                )
            )
            .filter(valid -> valid)
            .<Optional<? extends Content>>flatMapSingleElement(
                ignore -> rxsto.value(key).map(Optional::of)
            )
            .doOnError(err -> Logger.warn(this, "Failed to read cached item: %[exception]s", err))
            .onErrorComplete()
            .switchIfEmpty(
                SingleInterop.fromFuture(remote.get()).flatMap(
                    content -> {
                        final Single<Optional<? extends Content>> res;
                        if (content.isPresent()) {
                            res = rxsto.save(
                                key, new Content.From(content.get().size(), content.get())
                            ).andThen(rxsto.value(key)).map(Optional::of);
                        } else {
                            res = Single.fromCallable(Optional::empty);
                        }
                        return res;
                    }
                )
            ).to(SingleInterop.get());
    }
}
