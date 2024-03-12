/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.RepoName;
import com.artipie.docker.ManifestReference;

/**
 * Manifests layout in storage. Used to evaluate location for manifest link in storage.
 *
 * @since 0.7
 */
public interface ManifestsLayout {

    /**
     * Create manifest link key by it's reference.
     *
     * @param repo Repository name.
     * @param ref Manifest reference.
     * @return Key for storing manifest.
     */
    Key manifest(RepoName repo, ManifestReference ref);

    /**
     * Create tags key.
     *
     * @param repo Repository name.
     * @return Key for storing tags.
     */
    Key tags(RepoName repo);
}
