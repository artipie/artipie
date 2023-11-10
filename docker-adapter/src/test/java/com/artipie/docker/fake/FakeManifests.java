/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.fake;

import com.artipie.asto.Content;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Auxiliary class for tests for {@link com.artipie.docker.cache.CacheManifests}.
 *
 * @since 0.5
 */
public final class FakeManifests implements Manifests {
    /**
     * Manifests.
     */
    private final Manifests mnfs;

    /**
     * Ctor.
     *
     * @param type Type of manifests.
     * @param code Code of manifests.
     */
    public FakeManifests(final String type, final String code) {
        this.mnfs = manifests(type, code);
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
        return this.mnfs.put(ref, content);
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
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
        final Manifests manifests;
        switch (type) {
            case "empty":
                manifests = new EmptyGetManifests();
                break;
            case "full":
                manifests = new FullGetManifests(code);
                break;
            case "faulty":
                manifests = new FaultyGetManifests();
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported type: %s", type)
                );
        }
        return manifests;
    }
}
