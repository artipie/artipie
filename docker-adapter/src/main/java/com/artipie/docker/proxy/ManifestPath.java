/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.RepoName;
import com.artipie.docker.ref.ManifestRef;

/**
 * Path to manifest resource.
 *
 * @since 0.3
 */
final class ManifestPath {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Manifest reference.
     */
    private final ManifestRef ref;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param ref Manifest reference.
     */
    ManifestPath(final RepoName name, final ManifestRef ref) {
        this.name = name;
        this.ref = ref;
    }

    /**
     * Build path string.
     *
     * @return Path string.
     */
    public String string() {
        return String.format("/v2/%s/manifests/%s", this.name.value(), this.ref.string());
    }
}
