/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final Catalog ctlg;

    /**
     * From parameter captured.
     */
    private final AtomicReference<Optional<RepoName>> cfrom;

    /**
     * Limit parameter captured.
     */
    private final AtomicInteger climit;

    /**
     * Ctor.
     *
     * @param ctlg Catalog.
     */
    public FakeCatalogDocker(final Catalog ctlg) {
        this.ctlg = ctlg;
        this.cfrom = new AtomicReference<>();
        this.climit = new AtomicInteger();
    }

    /**
     * Get captured from parameter.
     *
     * @return Captured from parameter.
     */
    public Optional<RepoName> from() {
        return this.cfrom.get();
    }

    /**
     * Get captured limit parameter.
     *
     * @return Captured limit parameter.
     */
    public int limit() {
        return this.climit.get();
    }

    @Override
    public Repo repo(final RepoName name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> pfrom, final int plimit) {
        this.cfrom.set(pfrom);
        this.climit.set(plimit);
        return CompletableFuture.completedFuture(this.ctlg);
    }
}
