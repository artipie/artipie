/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;

/**
 * Path to blob resource.
 *
 * @since 0.3
 */
final class BlobPath {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Blob digest.
     */
    private final Digest digest;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param digest Blob digest.
     */
    BlobPath(final RepoName name, final Digest digest) {
        this.name = name;
        this.digest = digest;
    }

    /**
     * Build path string.
     *
     * @return Path string.
     */
    public String string() {
        return String.format("/v2/%s/blobs/%s", this.name.value(), this.digest.string());
    }
}
