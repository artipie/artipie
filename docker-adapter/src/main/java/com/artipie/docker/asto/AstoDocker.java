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
import com.artipie.docker.misc.Pagination;

import java.util.concurrent.CompletableFuture;

/**
 * Asto {@link Docker} implementation.
 */
public final class AstoDocker implements Docker {

    private final String registry;

    private final Storage storage;

    public AstoDocker(String registry, Storage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    @Override
    public String registry() {
        return registry;
    }

    @Override
    public Repo repo(String name) {
        return new AstoRepo(this.storage, name);
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        final Key root = Layout.repositories();
        return this.storage.list(root).thenApply(keys -> new AstoCatalog(root, keys, pagination));
    }
}
