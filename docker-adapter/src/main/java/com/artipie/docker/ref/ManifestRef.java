/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker.ref;

import com.artipie.asto.Key;
import com.artipie.docker.Digest;
import com.artipie.docker.Tag;
import java.util.Arrays;

/**
 * Manifest reference.
 * <p>
 * Can be resolved by image tag or digest.
 * </p>
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface ManifestRef {

    /**
     * Builds key for manifest blob link.
     *
     * @return Key to link.
     */
    Key link();

    /**
     * String representation.
     *
     * @return Reference as string.
     */
    String string();

    /**
     * Manifest reference from {@link Digest}.
     *
     * @since 0.2
     */
    final class FromDigest implements ManifestRef {

        /**
         * Digest.
         */
        private final Digest digest;

        /**
         * Ctor.
         *
         * @param digest Digest.
         */
        public FromDigest(final Digest digest) {
            this.digest = digest;
        }

        @Override
        public Key link() {
            return new Key.From(
                Arrays.asList("revisions", this.digest.alg(), this.digest.hex(), "link")
            );
        }

        @Override
        public String string() {
            return this.digest.string();
        }
    }

    /**
     * Manifest reference from {@link Tag}.
     *
     * @since 0.2
     */
    final class FromTag implements ManifestRef {

        /**
         * Tag.
         */
        private final Tag tag;

        /**
         * Ctor.
         *
         * @param tag Tag.
         */
        public FromTag(final Tag tag) {
            this.tag = tag;
        }

        @Override
        public Key link() {
            return new Key.From(
                Arrays.asList("tags", this.tag.value(), "current", "link")
            );
        }

        @Override
        public String string() {
            return this.tag.value();
        }
    }

    /**
     * Manifest reference from a string.
     * <p>
     * String may be tag or digest.
     *
     * @since 0.2
     */
    final class FromString implements ManifestRef {

        /**
         * Manifest reference string.
         */
        private final String value;

        /**
         * Ctor.
         *
         * @param value Manifest reference string.
         */
        public FromString(final String value) {
            this.value = value;
        }

        @Override
        public Key link() {
            final ManifestRef ref;
            final Digest.FromString digest = new Digest.FromString(this.value);
            final Tag.Valid tag = new Tag.Valid(this.value);
            if (digest.valid()) {
                ref = new FromDigest(digest);
            } else if (tag.valid()) {
                ref = new FromTag(tag);
            } else {
                throw new IllegalStateException(
                    String.format("Unsupported reference: `%s`", this.value)
                );
            }
            return ref.link();
        }

        @Override
        public String string() {
            return this.value;
        }
    }
}

