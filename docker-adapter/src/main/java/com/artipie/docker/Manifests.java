/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.asto.Content;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.Pagination;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Docker repository manifests.
 */
public interface Manifests {

    /**
     * Put manifest.
     *
     * @param ref     Manifest reference.
     * @param content Manifest content.
     * @return Added manifest.
     */
    CompletableFuture<Manifest> put(ManifestReference ref, Content content);

    /**
     * Get manifest by reference.
     *
     * @param ref Manifest reference
     * @return Manifest instance if it is found, empty if manifest is absent.
     */
    CompletableFuture<Optional<Manifest>> get(ManifestReference ref);

    /**
     * List manifest tags.
     *
     * @param pagination  Pagination parameters.
     * @return Tags.
     */
    CompletableFuture<Tags> tags(Pagination pagination);
}
