/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.artipie.RepoConfig;
import com.artipie.RepositoriesFromStorage;
import com.artipie.asto.Storage;
import com.artipie.management.Storages;
import java.util.concurrent.CompletionStage;

/**
 * Artipie {@link Storages} implementation.
 * @since 0.14
 */
public final class ArtipieStorages implements Storages {

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Artipie settings storage
     */
    public ArtipieStorages(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Storage> repoStorage(final String name) {
        return new RepositoriesFromStorage(this.storage).config(name)
            .thenApply(RepoConfig::storage);
    }
}
