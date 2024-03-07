/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Auxiliary class for tests for {@link com.artipie.docker.cache.CacheManifests}.
 */
public final class FakeManifests implements Manifests {

    private final Manifests mnfs;

    /**
     * @param type Type of manifests.
     * @param code Code of manifests.
     */
    public FakeManifests(final String type, final String code) {
        this.mnfs = manifests(type, code);
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestReference ref, final Content content) {
        return this.mnfs.put(ref, content);
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestReference ref) {
        return this.mnfs.get(ref);
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        return this.mnfs.tags(from, limit);
    }

    /**
     * Creates manifests.
     *
     * @param type Type of manifests.
     * @param code Code of manifests.
     * @return Manifests.
     */
    private static Manifests manifests(final String type, final String code) {
        return switch (type) {
            case "empty" -> new EmptyGetManifests();
            case "full" -> new FullGetManifests(code);
            case "faulty" -> new FaultyGetManifests();
            default -> throw new IllegalArgumentException("Unsupported type:" + type);
        };
    }
}
