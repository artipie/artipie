/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;

/**
 * Storage layout.
 * Provides location for all repository elements such as blobs, manifests and uploads.
 *
 * @since 0.7
 */
public interface Layout extends BlobsLayout, ManifestsLayout, UploadsLayout {

    /**
     * Create repositories key.
     *
     * @return Key for storing repositories.
     */
    Key repositories();
}
