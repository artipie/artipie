/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.asto.Key;
import com.artipie.docker.misc.Validator;

import java.util.Arrays;

/**
 * Manifest reference.
 * <p>Can be resolved by image tag or digest.
 *
 * @param link      The key for manifest blob link.
 * @param reference String representation.
 */
public record ManifestReference(Key link, String reference) {

    /**
     * Creates a manifest reference from a Content Digest.
     *
     * @param digest Content Digest
     * @return Manifest reference record
     */
    public static ManifestReference from(Digest digest) {
        return new ManifestReference(
            new Key.From(Arrays.asList("revisions", digest.alg(), digest.hex(), "link")),
            digest.string()
        );
    }

    /**
     * Creates a manifest reference from a string representation of Content Digest or Image Tag.
     *
     * @param val String representation of Content Digest or Image Tag
     * @return Manifest reference record
     */
    public static ManifestReference from(String val) {
        final Digest.FromString digest = new Digest.FromString(val);
        return digest.valid() ? from(digest) : fromTag(val);
    }

    /**
     * Creates a manifest reference from a Docker image tag.
     *
     * @param tag Image tag
     * @return Manifest reference record
     */
    public static ManifestReference fromTag(String tag) {
        String validated = Validator.validateTag(tag);
        return new ManifestReference(
            new Key.From(Arrays.asList("tags", validated, "current", "link")),
            validated
        );
    }
}
