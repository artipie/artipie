/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.Uploads;

/**
 * Read-write {@link Repo} implementation.
 *
 * @since 0.3
 */
public final class ReadWriteRepo implements Repo {

    /**
     * Repository for reading.
     */
    private final Repo read;

    /**
     * Repository for writing.
     */
    private final Repo write;

    /**
     * Ctor.
     *
     * @param read Repository for reading.
     * @param write Repository for writing.
     */
    public ReadWriteRepo(final Repo read, final Repo write) {
        this.read = read;
        this.write = write;
    }

    @Override
    public Layers layers() {
        return new ReadWriteLayers(this.read.layers(), this.write.layers());
    }

    @Override
    public Manifests manifests() {
        return new ReadWriteManifests(this.read.manifests(), this.write.manifests());
    }

    @Override
    public Uploads uploads() {
        return this.write.uploads();
    }
}
