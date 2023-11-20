/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Key;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import com.artipie.docker.asto.AstoBlob;
import com.artipie.docker.asto.BlobSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Layers implementation that contains blob.
 *
 * @since 0.3
 */
public final class FullGetLayers implements Layers {

    @Override
    public CompletionStage<Blob> put(final BlobSource source) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Blob> mount(final Blob blob) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        return CompletableFuture.completedFuture(
            Optional.of(new AstoBlob(new InMemoryStorage(), new Key.From("test"), digest))
        );
    }
}
