/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi-read {@link Repo} implementation.
 *
 * @since 0.3
 */
public final class MultiReadRepo implements Repo {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Repositories for reading.
     */
    private final List<Repo> repos;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param repos Repositories for reading.
     */
    public MultiReadRepo(final RepoName name, final List<Repo> repos) {
        this.name = name;
        this.repos = repos;
    }

    @Override
    public Layers layers() {
        return new MultiReadLayers(
            this.repos.stream().map(Repo::layers).collect(Collectors.toList())
        );
    }

    @Override
    public Manifests manifests() {
        return new MultiReadManifests(
            this.name, this.repos.stream().map(Repo::manifests).collect(Collectors.toList())
        );
    }

    @Override
    public Uploads uploads() {
        throw new UnsupportedOperationException();
    }
}
