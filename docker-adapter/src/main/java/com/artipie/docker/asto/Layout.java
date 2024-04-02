/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.Digest;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.RepoName;

/**
 * Original storage layout that is compatible with reference Docker Registry implementation.
 */
public final class Layout {

    public Key repositories() {
        return new Key.From("repositories");
    }

    public Key blob(final Digest digest) {
        return new Key.From(
            "blobs", digest.alg(), digest.hex().substring(0, 2), digest.hex(), "data"
        );
    }

    public Key manifest(final RepoName repo, final ManifestReference ref) {
        return new Key.From(this.manifests(repo), ref.link().string());
    }

    public Key tags(final RepoName repo) {
        return new Key.From(this.manifests(repo), "tags");
    }

    public Key upload(final RepoName name, final String uuid) {
        return Uploads.uploadKey(name, uuid);
    }

    /**
     * Create manifests root key.
     *
     * @param repo Repository name.
     * @return Manifests key.
     */
    private Key manifests(final RepoName repo) {
        return new Key.From(this.repositories(), repo.value(), "_manifests");
    }
}
