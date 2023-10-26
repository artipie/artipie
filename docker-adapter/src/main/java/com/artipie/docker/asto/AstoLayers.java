/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Layers}.
 *
 * @since 0.3
 */
public final class AstoLayers implements Layers {

    /**
     * Blobs storage.
     */
    private final BlobStore blobs;

    /**
     * Ctor.
     *
     * @param blobs Blobs storage.
     */
    public AstoLayers(final BlobStore blobs) {
        this.blobs = blobs;
    }

    @Override
    public CompletionStage<Blob> put(final BlobSource source) {
        return this.blobs.put(source);
    }

    @Override
    public CompletionStage<Blob> mount(final Blob blob) {
        return blob.content().thenCompose(
            content -> this.blobs.put(new TrustedBlobSource(content, blob.digest()))
        );
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        return this.blobs.blob(digest);
    }
}
