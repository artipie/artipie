/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker repository files and metadata.
 *
 * @since 0.3
 */
public interface Uploads {

    /**
     * Start new upload.
     *
     * @return Upload.
     */
    CompletionStage<Upload> start();

    /**
     * Find upload by UUID.
     *
     * @param uuid Upload UUID.
     * @return Upload.
     */
    CompletionStage<Optional<Upload>> get(String uuid);
}
