/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Storage;
import com.artipie.docker.Layers;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;

/**
 * Asto implementation of {@link Repo}.
 *
 * @since 0.1
 */
public final class AstoRepo implements Repo {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Storage layout.
     */
    private final Layout layout;

    /**
     * Ctor.
     *
     * @param asto Asto storage
     * @param layout Storage layout.
     * @param name Repository name
     */
    public AstoRepo(final Storage asto, final Layout layout, final RepoName name) {
        this.asto = asto;
        this.layout = layout;
        this.name = name;
    }

    @Override
    public Layers layers() {
        return new AstoLayers(this.blobs());
    }

    @Override
    public Manifests manifests() {
        return new AstoManifests(this.asto, this.blobs(), this.layout, this.name);
    }

    @Override
    public Uploads uploads() {
        return new AstoUploads(this.asto, this.layout, this.name);
    }

    /**
     * Get blobs storage.
     *
     * @return Blobs storage.
     */
    private AstoBlobs blobs() {
        return new AstoBlobs(this.asto, this.layout);
    }
}
