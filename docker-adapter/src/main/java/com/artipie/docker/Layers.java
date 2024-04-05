/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.asto.BlobSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Docker repository files and metadata.
 */
public interface Layers {

    /**
     * Add layer to repository.
     *
     * @param source Blob source.
     * @return Added layer blob.
     */
    CompletableFuture<Digest> put(BlobSource source);

    /**
     * Mount blob to repository.
     *
     * @param blob Blob.
     * @return Mounted blob.
     */
    CompletableFuture<Void> mount(Blob blob);

    /**
     * Find layer by digest.
     *
     * @param digest Layer digest.
     * @return Flow with manifest data, or empty if absent
     */
    CompletableFuture<Optional<Blob>> get(Digest digest);
}
