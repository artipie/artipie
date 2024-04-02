/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manifests implementation that fails to get manifest.
 */
public final class FaultyGetManifests implements Manifests {

    @Override
    public CompletableFuture<Manifest> put(final ManifestReference ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Optional<Manifest>> get(final ManifestReference ref) {
        return new FailedCompletionStage<>(new IllegalStateException());
    }

    @Override
    public CompletableFuture<Tags> tags(final Optional<Tag> from, final int limit) {
        throw new UnsupportedOperationException();
    }
}
