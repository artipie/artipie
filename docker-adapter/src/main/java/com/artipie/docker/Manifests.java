/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.asto.Content;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker repository manifests.
 *
 * @since 0.3
 */
public interface Manifests {

    /**
     * Put manifest.
     *
     * @param ref Manifest reference.
     * @param content Manifest content.
     * @return Added manifest.
     */
    CompletionStage<Manifest> put(ManifestRef ref, Content content);

    /**
     * Get manifest by reference.
     *
     * @param ref Manifest reference
     * @return Manifest instance if it is found, empty if manifest is absent.
     */
    CompletionStage<Optional<Manifest>> get(ManifestRef ref);

    /**
     * List manifest tags.
     *
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @return Tags.
     */
    CompletionStage<Tags> tags(Optional<Tag> from, int limit);

    /**
     * Abstract decorator for Manifests.
     *
     * @since 0.3
     */
    abstract class Wrap implements Manifests {

        /**
         * Origin manifests.
         */
        private final Manifests manifests;

        /**
         * Ctor.
         *
         * @param manifests Manifests.
         */
        protected Wrap(final Manifests manifests) {
            this.manifests = manifests;
        }

        @Override
        public final CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
            return this.manifests.put(ref, content);
        }

        @Override
        public final CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
            return this.manifests.get(ref);
        }

        @Override
        public final CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
            return this.manifests.tags(from, limit);
        }
    }
}
