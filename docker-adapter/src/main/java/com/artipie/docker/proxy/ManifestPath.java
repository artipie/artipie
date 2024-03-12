/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.artipie.docker.ManifestReference;

/**
 * Path to manifest resource.
 */
final class ManifestPath {

    /**
     * Repository name.
     */
    private final RepoName name;

    private final ManifestReference ref;

    /**
     * @param name Repository name.
     * @param ref Manifest reference.
     */
    ManifestPath(final RepoName name, final ManifestReference ref) {
        this.name = name;
        this.ref = ref;
    }

    /**
     * Build path string.
     *
     * @return Path string.
     */
    public String string() {
        return String.format("/v2/%s/manifests/%s", this.name.value(), this.ref.reference());
    }
}
