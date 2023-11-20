/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Copy;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.KeyLastPart;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Maven front for artipie maven adaptor.
 * @since 0.5
 */
public interface Maven {

    /**
     * Updates the metadata of a maven package.
     * @param upload Uploading artifact location
     * @param artifact Artifact location
     * @return Completion stage
     */
    CompletionStage<Void> update(Key upload, Key artifact);

    /**
     * Fake {@link Maven} implementation.
     * @since 0.5
     */
    class Fake implements Maven {

        /**
         * Was maven updated?
         */
        private boolean updated;

        /**
         * Test storage.
         */
        private final Storage asto;

        /**
         * Ctor.
         * @param asto Test storage
         */
        public Fake(final Storage asto) {
            this.asto = asto;
        }

        @Override
        public CompletionStage<Void> update(final Key upload, final Key artifact) {
            this.updated = true;
            new Copy(new SubStorage(upload, this.asto)).copy(
                new SubStorage(new Key.From(artifact, new KeyLastPart(upload).get()), this.asto)
            ).join();
            return CompletableFuture.allOf();
        }

        /**
         * Was maven updated?
         * @return True is was, false - otherwise
         */
        public boolean wasUpdated() {
            return this.updated;
        }
    }
}
