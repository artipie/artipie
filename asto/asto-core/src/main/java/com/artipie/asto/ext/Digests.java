/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Common digests.
 * @since 0.22
 */
public enum Digests implements Supplier<MessageDigest> {
    /**
     * Common digest algorithms.
     */
    SHA256("SHA-256"), SHA1("SHA-1"), MD5("MD5"), SHA512("SHA-512");

    /**
     * Digest name.
     */
    private final String name;

    /**
     * New digest for name.
     * @param name Digest name
     */
    Digests(final String name) {
        this.name = name;
    }

    @Override
    public MessageDigest get() {
        try {
            return MessageDigest.getInstance(this.name);
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException(String.format("No algorithm '%s'", this.name), err);
        }
    }

    /**
     * Digest enum item from string digest algorithm, case insensitive.
     * @since 0.24
     */
    public static final class FromString {

        /**
         * Algorithm string representation.
         */
        private final String from;

        /**
         * Ctor.
         * @param from Algorithm string representation
         */
        public FromString(final String from) {
            this.from = from;
        }

        /**
         * Returns {@link Digests} enum item.
         * @return Digest
         */
        public Digests get() {
            return Stream.of(Digests.values()).filter(
                digest -> digest.name.equalsIgnoreCase(this.from)
            ).findFirst().orElseThrow(
                () -> new IllegalArgumentException(
                    String.format("Unsupported digest algorithm %s", this.from)
                )
            );
        }
    }
}
