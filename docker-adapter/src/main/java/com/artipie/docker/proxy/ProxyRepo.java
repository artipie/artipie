/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;
import com.artipie.http.Slice;

/**
 * Proxy implementation of {@link Repo}.
 *
 * @since 0.3
 */
public final class ProxyRepo implements Repo {

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     * @param name Repository name.
     */
    public ProxyRepo(final Slice remote, final RepoName name) {
        this.remote = remote;
        this.name = name;
    }

    @Override
    public Layers layers() {
        return new ProxyLayers(this.remote, this.name);
    }

    @Override
    public Manifests manifests() {
        return new ProxyManifests(this.remote, this.name);
    }

    @Override
    public Uploads uploads() {
        throw new UnsupportedOperationException();
    }
}
