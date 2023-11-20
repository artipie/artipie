/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.RepoName;

/**
 * Uploads layout in storage. Used to evaluate location for uploads in storage.
 *
 * @since 0.7
 */
public interface UploadsLayout {

    /**
     * Create upload key by it's UUID.
     *
     * @param repo Repository name.
     * @param uuid Manifest reference.
     * @return Key for storing upload.
     */
    Key upload(RepoName repo, String uuid);
}
