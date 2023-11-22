/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.asto.BlobSource;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker repository files and metadata.
 *
 * @since 0.3
 */
public interface Layers {

    /**
     * Add layer to repository.
     *
     * @param source Blob source.
     * @return Added layer blob.
     */
    CompletionStage<Blob> put(BlobSource source);

    /**
     * Mount blob to repository.
     *
     * @param blob Blob.
     * @return Mounted blob.
     */
    CompletionStage<Blob> mount(Blob blob);

    /**
     * Find layer by digest.
     *
     * @param digest Layer digest.
     * @return Flow with manifest data, or empty if absent
     */
    CompletionStage<Optional<Blob>> get(Digest digest);

    /**
     * Abstract decorator for Layers.
     *
     * @since 0.3
     */
    abstract class Wrap implements Layers {

        /**
         * Origin layers.
         */
        private final Layers layers;

        /**
         * Ctor.
         *
         * @param layers Layers.
         */
        protected Wrap(final Layers layers) {
            this.layers = layers;
        }

        @Override
        public final CompletionStage<Blob> put(final BlobSource source) {
            return this.layers.put(source);
        }

        @Override
        public final CompletionStage<Optional<Blob>> get(final Digest digest) {
            return this.layers.get(digest);
        }
    }
}
