/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.docker.ManifestReference;

/**
 * Original storage layout that is compatible with reference Docker Registry implementation.
 *
 * @since 0.7
 */
public final class DefaultLayout implements Layout {

    @Override
    public Key repositories() {
        return new Key.From("repositories");
    }

    @Override
    public Key blob(final RepoName repo, final Digest digest) {
        return new BlobKey(digest);
    }

    @Override
    public Key manifest(final RepoName repo, final ManifestReference ref) {
        return new Key.From(this.manifests(repo), ref.link().string());
    }

    @Override
    public Key tags(final RepoName repo) {
        return new Key.From(this.manifests(repo), "tags");
    }

    @Override
    public Key upload(final RepoName repo, final String uuid) {
        return new UploadKey(repo, uuid);
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
