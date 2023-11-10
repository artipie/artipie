/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker registry blob store.
 * @since 0.1
 */
public interface BlobStore {

    /**
     * Load blob by digest.
     * @param digest Blob digest
     * @return Async publisher output
     */
    CompletionStage<Optional<Blob>> blob(Digest digest);

    /**
     * Put blob into the store from source.
     *
     * @param source Blob source.
     * @return Added blob.
     */
    CompletionStage<Blob> put(BlobSource source);
}

