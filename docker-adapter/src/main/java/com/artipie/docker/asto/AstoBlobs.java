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
import java.util.concurrent.CompletionStage;

/**
 * Asto {@link BlobStore} implementation.
 */
public final class AstoBlobs implements BlobStore {

    /**
     * Storage.
     */
    private final Storage asto;

    /**
     * @param asto Storage
     */
    public AstoBlobs(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public CompletableFuture<Optional<Blob>> blob(final Digest digest) {
        final Key key = Layout.blob(digest);
        return this.asto.exists(key).thenApply(
            exists -> {
                final Optional<Blob> blob;
                if (exists) {
                    blob = Optional.of(new AstoBlob(this.asto, key, digest));
                } else {
                    blob = Optional.empty();
                }
                return blob;
            }
        );
    }

    @Override
    public CompletionStage<Blob> put(final BlobSource source) {
        final Digest digest = source.digest();
        final Key key = Layout.blob(digest);
        return source.saveTo(this.asto, key)
            .thenApply(
                nothing -> new AstoBlob(this.asto, key, digest)
            );
    }
}
