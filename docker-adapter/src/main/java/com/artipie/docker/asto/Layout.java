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

    public static Key repositories() {
        return new Key.From("repositories");
    }

    public static Key blob(Digest digest) {
        return new Key.From(
            "blobs", digest.alg(), digest.hex().substring(0, 2), digest.hex(), "data"
        );
    }

    public static Key manifest(RepoName repo, final ManifestReference ref) {
        return new Key.From(manifests(repo), ref.link().string());
    }

    public static Key tags(RepoName repo) {
        return new Key.From(manifests(repo), "tags");
    }

    public static Key upload(RepoName name, final String uuid) {
        return new Key.From(repositories(), name.value(), "_uploads", uuid);
    }

    /**
     * Create manifests root key.
     *
     * @param repo Repository name.
     * @return Manifests key.
     */
    private static Key manifests(final RepoName repo) {
        return new Key.From(repositories(), repo.value(), "_manifests");
    }
}
