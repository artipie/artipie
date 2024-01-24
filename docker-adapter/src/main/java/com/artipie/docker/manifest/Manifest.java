/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Image manifest.
 * See <a href="https://docs.docker.com/engine/reference/commandline/manifest/">docker manifest</a>
 *
 * @since 0.2
 */
public interface Manifest {

    /**
     * Read manifest types.
     *
     * @return Type string.
     */
    Set<String> mediaTypes();

    /**
     * Converts manifest to one of types.
     *
     * @param options Types the manifest may be converted to.
     * @return Converted manifest.
     */
    Manifest convert(Set<? extends String> options);

    /**
     * Read config digest.
     *
     * @return Config digests.
     */
    Digest config();

    /**
     * Read layer digests.
     *
     * @return Layer digests.
     */
    Collection<Layer> layers();

    /**
     * Manifest digest.
     *
     * @return Digest.
     */
    Digest digest();

    /**
     * Read manifest binary content.
     *
     * @return Manifest binary content.
     */
    Content content();

    /**
     * Manifest size.
     *
     * @return Size of the manifest.
     */
    long size();

    /**
     * Read manifest first media type.
     * @return First media type in a collection
     * @deprecated Use {@link #mediaTypes()} instead, this method
     *  will be removed in next major release.
     */
    @Deprecated
    default String mediaType() {
        return this.mediaTypes().iterator().next();
    }

    /**
     * Converts manifest to one of types.
     *
     * @param options Types the manifest may be converted to.
     * @return Converted manifest.
     * @deprecated Use {@link #convert(Set)} instead, this method
     *  will be removed in next major release.
     */
    @Deprecated
    default Manifest convert(final List<String> options) {
        return this.convert(new HashSet<>(options));
    }
}
