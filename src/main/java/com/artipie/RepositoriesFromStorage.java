/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.repo.ConfigFile;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories created from {@link Settings}.
 *
 * @since 0.13
 */
public final class RepositoriesFromStorage implements Repositories {

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
        final Key keyname = new Key.From(name);
        return Single.zip(
            SingleInterop.fromFuture(
                new ConfigFile(keyname).valueFrom(this.storage)
            ),
            SingleInterop.fromFuture(StorageAliases.find(this.storage, keyname)),
            (data, aliases) -> SingleInterop.fromFuture(
                RepoConfig.fromPublisher(aliases, keyname, data)
            )
        ).flatMap(self -> self).to(SingleInterop.get());
    }
}
