/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;
import com.artipie.scheduling.ArtifactEvent;
import java.util.Optional;
import java.util.Queue;

/**
 * Cache implementation of {@link Repo}.
 *
 * @since 0.3
 */
public final class CacheRepo implements Repo {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Origin repository.
     */
    private final Repo origin;

    /**
     * Cache repository.
     */
    private final Repo cache;

    /**
     * Events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Artipie repository name.
     */
    private final String rname;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact events
     * @param rname Repository name
         */
    public CacheRepo(final RepoName name, final Repo origin, final Repo cache,
        final Optional<Queue<ArtifactEvent>> events, final String rname) {
        this.name = name;
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Layers layers() {
        return new CacheLayers(this.origin.layers(), this.cache.layers());
    }

    @Override
    public Manifests manifests() {
        return new CacheManifests(this.name, this.origin, this.cache, this.events, this.rname);
    }

    @Override
    public Uploads uploads() {
        throw new UnsupportedOperationException();
    }
}
