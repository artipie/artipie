/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Docker registry storage main object.
 * @see com.artipie.docker.asto.AstoDocker
 * @since 0.1
 */
public interface Docker {

    /**
     * Docker repo by name.
     * @param name Repository name
     * @return Repository object
     */
    Repo repo(RepoName name);

    /**
     * Docker repositories catalog.
     *
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @return Catalog.
     */
    CompletionStage<Catalog> catalog(Optional<RepoName> from, int limit);
}
