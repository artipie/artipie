/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;

/**
 * Blobs layout in storage. Used to evaluate location for blobs in storage.
 *
 * @since 0.7
 */
public interface BlobsLayout {

    /**
     * Get blob key by it's digest.
     *
     * @param repo Repository name.
     * @param digest Blob digest.
     * @return Key for storing blob.
     */
    Key blob(RepoName repo, Digest digest);
}
