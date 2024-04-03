/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.Pagination;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Docker implementation with specified catalog.
 * Values of parameters `from` and `limit` from last call of `catalog` method are captured.
 *
 * @since 0.10
 */
public final class FakeCatalogDocker implements Docker {

    /**
     * Catalog.
     */
    private final Catalog catalog;

    /**
     * From parameter captured.
     */
    private final AtomicReference<Pagination> paginationRef;

    public FakeCatalogDocker(Catalog catalog) {
        this.catalog = catalog;
        this.paginationRef = new AtomicReference<>();
    }

    /**
     * Get captured from parameter.
     *
     * @return Captured from parameter.
     */
    public RepoName from() {
        return this.paginationRef.get().last();
    }

    /**
     * Get captured limit parameter.
     *
     * @return Captured limit parameter.
     */
    public int limit() {
        return this.paginationRef.get().limit();
    }

    @Override
    public Repo repo(final RepoName name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        this.paginationRef.set(pagination);
        return CompletableFuture.completedFuture(this.catalog);
    }
}
