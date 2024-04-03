/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.Pagination;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asto {@link Docker} implementation.
 */
public final class AstoDocker implements Docker {

    /**
     * Asto storage.
     */
    private final Storage storage;

    /**
     * @param storage Storage.
     */
    public AstoDocker(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Repo repo(RepoName name) {
        return new AstoRepo(this.storage, name);
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        final Key root = Layout.repositories();
        return this.storage.list(root).thenApply(keys -> new AstoCatalog(root, keys, pagination));
    }
}
