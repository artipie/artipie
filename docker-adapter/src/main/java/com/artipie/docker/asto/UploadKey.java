/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.RepoName;

/**
 * Key of blob upload root.
 *
 * @since 0.3
 */
final class UploadKey extends Key.Wrap {

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param uuid Upload UUID.
     */
    UploadKey(final RepoName name, final String uuid) {
        super(
            new Key.From(
                "repositories", name.value(), "_uploads", uuid
            )
        );
    }
}
