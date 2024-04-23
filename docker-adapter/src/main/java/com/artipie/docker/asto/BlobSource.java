/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Digest;

import java.util.concurrent.CompletableFuture;

/**
 * Source of blob that could be saved to {@link Storage} at desired location.
 *
 * @since 0.12
 */
public interface BlobSource {

    /**
     * Blob digest.
     *
     * @return Digest.
     */
    Digest digest();

    /**
     * Save blob to storage.
     *
     * @param storage Storage.
     * @param key     Destination for blob content.
     * @return Completion of save operation.
     */
    CompletableFuture<Void> saveTo(Storage storage, Key key);
}
