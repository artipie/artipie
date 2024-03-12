/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.JoinedTagsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Multi-read {@link Manifests} implementation.
 */
public final class MultiReadManifests implements Manifests {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiReadManifests.class);

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Manifests for reading.
     */
    private final List<Manifests> manifests;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param manifests Manifests for reading.
     */
    public MultiReadManifests(final RepoName name, final List<Manifests> manifests) {
        this.name = name;
        this.manifests = manifests;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestReference ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestReference ref) {
        return CompletableFuture.supplyAsync(() -> {
            for (Manifests m : manifests) {
                Optional<Manifest> res = m.get(ref).handle(
                    (manifest, throwable) -> {
                        final Optional<Manifest> result;
                        if (throwable == null) {
                            result = manifest;
                        } else {
                            LOGGER.error("Failed to read manifest " + ref.reference(), throwable);
                            result = Optional.empty();
                        }
                        return result;
                    }
                ).toCompletableFuture().join();
                if (res.isPresent()) {
                    return res;
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        return new JoinedTagsSource(this.name, this.manifests, from, limit).tags();
    }
}
