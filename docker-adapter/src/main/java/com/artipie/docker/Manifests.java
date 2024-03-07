/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.asto.Content;
import com.artipie.docker.manifest.Manifest;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker repository manifests.
 */
public interface Manifests {

    /**
     * Put manifest.
     *
     * @param ref Manifest reference.
     * @param content Manifest content.
     * @return Added manifest.
     */
    CompletionStage<Manifest> put(ManifestReference ref, Content content);

    /**
     * Get manifest by reference.
     *
     * @param ref Manifest reference
     * @return Manifest instance if it is found, empty if manifest is absent.
     */
    CompletionStage<Optional<Manifest>> get(ManifestReference ref);

    /**
     * List manifest tags.
     *
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @return Tags.
     */
    CompletionStage<Tags> tags(Optional<Tag> from, int limit);
}
