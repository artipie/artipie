/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.cache;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.asto.Uploads;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;

/**
 * Cache implementation of {@link Repo}.
 */
public final class CacheRepo implements Repo {

    /**
     * Repository name.
     */
    private final String name;

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
    private final String repoName;

    /**
     * @param name Repository name.
     * @param origin Origin repository.
     * @param cache Cache repository.
     * @param events Artifact events.
     * @param registryName Registry name.
     */
    public CacheRepo(String name, Repo origin, Repo cache,
                     Optional<Queue<ArtifactEvent>> events, String registryName) {
        this.name = name;
        this.origin = origin;
        this.cache = cache;
        this.events = events;
        this.repoName = registryName;
    }

    @Override
    public Layers layers() {
        return new CacheLayers(this.origin.layers(), this.cache.layers());
    }

    @Override
    public Manifests manifests() {
        return new CacheManifests(this.name, this.origin, this.cache, this.events, this.repoName);
    }

    @Override
    public Uploads uploads() {
        throw new UnsupportedOperationException();
    }
}
