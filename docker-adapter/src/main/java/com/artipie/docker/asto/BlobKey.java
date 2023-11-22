/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.docker.Digest;

/**
 * Key for blob data in storage.
 *
 * @since 0.2
 */
final class BlobKey extends Key.Wrap {

    /**
     * Ctor.
     *
     * @param digest Blob digest
     */
    BlobKey(final Digest digest) {
        super(
            new Key.From(
                "blobs", digest.alg(), digest.hex().substring(0, 2), digest.hex(), "data"
            )
        );
    }
}
