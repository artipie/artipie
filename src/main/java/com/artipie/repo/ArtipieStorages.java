/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.artipie.RepoConfig;
import com.artipie.RepositoriesFromStorage;
import com.artipie.asto.Storage;
import com.artipie.http.client.ClientSlices;
import com.artipie.management.Storages;
import java.util.concurrent.CompletionStage;

/**
 * Artipie {@link Storages} implementation.
 * @since 0.14
 */
public final class ArtipieStorages implements Storages {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param http HTTP client
     * @param storage Artipie settings storage
     */
    public ArtipieStorages(final ClientSlices http, final Storage storage) {
        this.http = http;
        this.storage = storage;
    }

    @Override
    public CompletionStage<Storage> repoStorage(final String name) {
        return new RepositoriesFromStorage(this.http, this.storage).config(name)
            .thenApply(RepoConfig::storage);
    }
}
