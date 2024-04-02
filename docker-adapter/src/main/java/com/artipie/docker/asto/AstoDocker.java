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
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Asto {@link Docker} implementation.
 */
public final class AstoDocker implements Docker {

    /**
     * Asto storage.
     */
    private final Storage storage;

    /**
     * Storage layout.
     */
    private final Layout layout;

    /**
     * Ctor.
     * @param storage Asto storage
     */
    public AstoDocker(final Storage storage) {
        this(storage, new Layout());
    }

    /**
     * @param storage Storage.
     * @param layout Storage layout.
     */
    public AstoDocker(Storage storage, Layout layout) {
        this.storage = storage;
        this.layout = layout;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new AstoRepo(this.storage, this.layout, name);
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        final Key root = this.layout.repositories();
        return this.storage.list(root).thenApply(keys -> new AstoCatalog(root, keys, from, limit));
    }
}
