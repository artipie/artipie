/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
 * @since 0.1
 */
public final class AstoDocker implements Docker {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Storage layout.
     */
    private final Layout layout;

    /**
     * Ctor.
     * @param asto Asto storage
     */
    public AstoDocker(final Storage asto) {
        this(asto, new DefaultLayout());
    }

    /**
     * Ctor.
     *
     * @param asto Storage.
     * @param layout Storage layout.
     */
    public AstoDocker(final Storage asto, final Layout layout) {
        this.asto = asto;
        this.layout = layout;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new AstoRepo(this.asto, this.layout, name);
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        final Key root = this.layout.repositories();
        return this.asto.list(root).thenApply(keys -> new AstoCatalog(root, keys, from, limit));
    }
}
