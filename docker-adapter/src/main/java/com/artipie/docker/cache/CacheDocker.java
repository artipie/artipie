/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.JoinedCatalogSource;
import com.artipie.scheduling.ArtifactEvent;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletionStage;

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
     * Artipie repository name.
     */
    private final String rname;

    /**
     * Ctor.
     *
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact metadata events queue
     * @param rname Artipie repository name
     */
    public CacheDocker(final Docker origin, final Docker cache,
        final Optional<Queue<ArtifactEvent>> events, final String rname) {
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new CacheRepo(
            name, this.origin.repo(name), this.cache.repo(name), this.events, this.rname
        );
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return new JoinedCatalogSource(from, limit, this.origin, this.cache).catalog();
    }
}
