/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Key;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Valid upload to maven repository.
 * @since 0.5
 */
public interface ValidUpload {

    /**
     * Validate upload:
     * - validate upload checksums;
     * - validate metadata: check metadata group and id are the same as in
     * repository metadata, metadata versions are correct.
     * @param upload Uploading artifact location
     * @param artifact Artifact location
     * @return Completable validation action: true if uploaded maven-metadata.xml is valid,
     *  false otherwise
     */
    CompletionStage<Boolean> validate(Key upload, Key artifact);

    /**
     * Is the upload ready to be added to repository? The upload is considered to be ready if
     * at an artifact (any, nondeterministic) and maven-metadata.xml have the same set of checksums.
     * @param location Upload location to check
     * @return Completable action with the result
     */
    CompletionStage<Boolean> ready(Key location);

    /**
     * Dummy {@link ValidUpload} implementation.
     * @since 0.5
     */
    final class Dummy implements ValidUpload {

        /**
         * Validation result.
         */
        private final boolean valid;

        /**
         * Is upload ready?
         */
        private final boolean rdy;

        /**
         * Ctor.
         * @param valid Result of the validation
         * @param ready Is upload ready?
         */
        public Dummy(final boolean valid, final boolean ready) {
            this.valid = valid;
            this.rdy = ready;
        }

        /**
         * Ctor.
         * @param valid Result of the validation
         */
        public Dummy(final boolean valid) {
            this(valid, true);
        }

        /**
         * Ctor.
         */
        public Dummy() {
            this(true, true);
        }

        @Override
        public CompletionStage<Boolean> validate(final Key upload, final Key artifact) {
            return CompletableFuture.completedFuture(this.valid);
        }

        @Override
        public CompletionStage<Boolean> ready(final Key location) {
            return CompletableFuture.completedFuture(this.rdy);
        }

    }

}
