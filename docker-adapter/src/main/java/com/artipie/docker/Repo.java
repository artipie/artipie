/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.asto.Uploads;

/**
 * Docker repository files and metadata.
 * @since 0.1
 */
public interface Repo {

    /**
     * Repository layers.
     *
     * @return Layers.
     */
    Layers layers();

    /**
     * Repository manifests.
     *
     * @return Manifests.
     */
    Manifests manifests();

    /**
     * Repository uploads.
     *
     * @return Uploads.
     */
    Uploads uploads();
}
