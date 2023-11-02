/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Supported algorithms for hashing.
 *
 * @since 0.3.3
 */
public enum Digest {
    /**
     * Supported algorithm enumeration: SHA-1, SHA-256.
     */
    SHA1("SHA-1", "sha"), SHA256("SHA-256", "sha256");

    /**
     * Algorithm used to instantiate MessageDigest instance.
     */
    private final String hashalg;

    /**
     * Algorithm name used in RPM metadata as checksum type.
     */
    private final String type;

    /**
     * Ctor.
     * @param alg Hashing algorithm
     * @param type Short name of the algorithm used in RPM metadata.
     */
    Digest(final String alg, final String type) {
        this.hashalg = alg;
        this.type = type;
    }

    /**
     * Instantiate MessageDigest instance.
     * @return MessageDigest instance
     */
    public MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance(this.hashalg);
        } catch (final NoSuchAlgorithmException err) {
            throw new IllegalStateException(
                String.format(
                    "%s is unavailable on this environment",
                    this.hashalg
                ),
                err
            );
        }
    }

    /**
     * Returns short algorithm name for using in RPM metadata.
     * @return Digest type
     */
    public String type() {
        return this.type;
    }
}
