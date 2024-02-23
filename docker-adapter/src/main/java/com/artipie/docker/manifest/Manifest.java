/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * An image manifest representation.
 * <p>See <a href="https://distribution.github.io/distribution/#image-manifest">Image manifest</a>
 * <p>See <a href="https://github.com/opencontainers/image-spec/tree/main">Open Container Initiative (OCI) Spec</a>
 */
public interface Manifest {

    /**
     * New image manifest format (schemaVersion = 2).
     */
    String MIME_V2_MANIFEST_SCHEMA2 = "application/vnd.docker.distribution.manifest.v2+json";

    /**
     * Image Manifest OCI Specification.
     */
    String MIME_OCI_V1_MANIFEST = "application/vnd.oci.image.manifest.v1+json";

    /**
     * Read manifest types.
     *
     * @return Type string.
     */
    @Deprecated
    default Set<String> mediaTypes() {
        return Collections.singleton(this.mediaType());
    }

    /**
     * The MIME type of the manifest.
     *
     * @return The MIME type.
     */
    String mediaType();

    /**
     * Converts manifest to one of types.
     *
     * @param options Types the manifest may be converted to.
     * @return Converted manifest.
     * @Depricated There is no needing to convert manifests.
     * Converting of manifests is the docker client's responsibility.
     */
    @Deprecated
    default Manifest convert(Set<? extends String> options) {
        return this;
    }

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
}
