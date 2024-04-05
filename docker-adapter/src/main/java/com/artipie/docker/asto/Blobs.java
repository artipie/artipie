/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Docker registry blob store.
 */
public final class Blobs {

    private final Storage storage;

    /**
     * @param storage Storage
     */
    public Blobs(Storage storage) {
        this.storage = storage;
    }

    /**
     * Load blob by digest.
     *
     * @param digest Blob digest
     * @return Async publisher output
     */
    public CompletableFuture<Optional<Blob>> blob(Digest digest) {
        final Key key = Layout.blob(digest);
        return storage.exists(key)
            .thenApply(
                exists -> exists
                    ? Optional.of(new AstoBlob(storage, key, digest))
                    : Optional.empty()
        );
    }

    /**
     * Put blob into the store from source.
     *
     * @param source Blob source.
     * @return Added blob.
     */
    public CompletableFuture<Digest> put(BlobSource source) {
        final Digest digest = source.digest();
        final Key key = Layout.blob(digest);
        return source.saveTo(storage, key)
            .thenApply(nothing -> digest);
    }
}
