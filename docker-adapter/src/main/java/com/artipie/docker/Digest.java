/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Content Digest.
 * See <a href="https://docs.docker.com/registry/spec/api/#content-digests">Content Digests</a>
 *
 * @since 0.1
 */
public interface Digest {

    /**
     * Digest algorithm part.
     * @return Algorithm string
     */
    String alg();

    /**
     * Digest hex.
     * @return Link digest hex string
     */
    String hex();

    /**
     * Digest string.
     * @return Digest string representation
     */
    default String string() {
        return String.format("%s:%s", this.alg(), this.hex());
    }

    /**
     * SHA256 digest implementation.
     * @since 0.1
     */
    final class Sha256 implements Digest {

        /**
         * SHA256 hex string.
         */
        private final String hex;

        /**
         * Ctor.
         * @param hex SHA256 hex string
         */
        public Sha256(final String hex) {
            this.hex = hex;
        }

        /**
         * Ctor.
         * @param bytes Data to calculate SHA256 digest hex
         */
        public Sha256(final byte[] bytes) {
            this(DigestUtils.sha256Hex(bytes));
        }

        @Override
        public String alg() {
            return "sha256";
        }

        @Override
        public String hex() {
            return this.hex;
        }

        @Override
        public String toString() {
            return this.string();
        }
    }

    /**
     * Digest parsed from string.
     * <p>
     * See <a href="https://docs.docker.com/registry/spec/api/#content-digests">Content Digests</a>
     * <p>
     * Docker registry digest is a string with digest formatted
     * by joining algorithm name with hex string using {@code :} as separator.
     * E.g. if algorithm is {@code sha256} and the digest is {@code 0000}, the link will be
     * {@code sha256:0000}.
     * @since 0.1
     */
    final class FromString implements Digest {

        /**
         * Digest string.
         */
        private final String original;

        /**
         * Ctor.
         *
         * @param original Digest string.
         */
        public FromString(final String original) {
            this.original = original;
        }

        @Override
        public String alg() {
            return this.part(0);
        }

        @Override
        public String hex() {
            return this.part(1);
        }

        @Override
        public String toString() {
            return this.original;
        }

        /**
         * Validates digest string.
         *
         * @return True if string is valid digest, false otherwise.
         */
        public boolean valid() {
            return this.original.split(":").length == 2;
        }

        /**
         * Part from input string split by {@code :}.
         * @param pos Part position
         * @return Part
         */
        private String part(final int pos) {
            if (!this.valid()) {
                throw new IllegalStateException(
                    String.format(
                        "Expected two parts separated by `:`, but was `%s`", this.original
                    )
                );
            }
            return this.original.split(":")[pos];
        }
    }
}
