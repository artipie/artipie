/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asto implementation of {@link Layers}.
 */
public final class AstoLayers implements Layers {

    /**
     * Blobs storage.
     */
    private final Blobs blobs;

    /**
     * @param blobs Blobs storage.
     */
    public AstoLayers(final Blobs blobs) {
        this.blobs = blobs;
    }

    @Override
    public CompletableFuture<Blob> put(final BlobSource source) {
        return this.blobs.put(source);
    }

    @Override
    public CompletableFuture<Blob> mount(Blob blob) {
        return blob.content()
            .thenCompose(content -> blobs.put(new TrustedBlobSource(content, blob.digest())));
    }

    @Override
    public CompletableFuture<Optional<Blob>> get(final Digest digest) {
        return this.blobs.blob(digest);
    }
}
