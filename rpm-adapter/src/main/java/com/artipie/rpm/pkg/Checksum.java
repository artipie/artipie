/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import java.io.IOException;

/**
 * RPM checksum.
 * @since 0.6
 */
public interface Checksum {

    /**
     * Digest.
     * @return Digest
     */
    Digest digest();

    /**
     * Checksum hex string.
     * @return Hex string
     * @throws IOException On error
     */
    String hex() throws IOException;

    /**
     * Simple {@link Checksum} implementation.
     * @since 0.11
     */
    final class Simple implements Checksum {

        /**
         * Digest.
         */
        private final Digest dgst;

        /**
         * Checksum hex.
         */
        private final String sum;

        /**
         * Ctor.
         * @param dgst Digest
         * @param sum Checksum hex
         */
        public Simple(final Digest dgst, final String sum) {
            this.dgst = dgst;
            this.sum = sum;
        }

        @Override
        public Digest digest() {
            return this.dgst;
        }

        @Override
        public String hex() throws IOException {
            return this.sum;
        }
    }
}
