/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.misc.JoinedCatalogSource;
import com.artipie.docker.misc.Pagination;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * Cache {@link Docker} implementation.
 *
 * @since 0.3
 */
public final class CacheDocker implements Docker {

    /**
     * Origin repository.
     */
    private final Docker origin;

    /**
     * Cache repository.
     */
    private final Docker cache;

    /**
     * Artifact metadata events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact metadata events queue
     */
    public CacheDocker(Docker origin,
                       Docker cache,
                       Optional<Queue<ArtifactEvent>> events
    ) {
        this.origin = origin;
        this.cache = cache;
        this.events = events;
    }

    @Override
    public String registry() {
        return origin.registry();
    }

    @Override
    public Repo repo(final String name) {
        return new CacheRepo(name, this.origin.repo(name), this.cache.repo(name), this.events, registry());
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        return new JoinedCatalogSource(pagination, this.origin, this.cache).catalog();
    }
}
