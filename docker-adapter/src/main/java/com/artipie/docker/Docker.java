/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker;

import com.artipie.docker.misc.Pagination;

import java.util.concurrent.CompletableFuture;

/**
 * Docker registry storage main object.
 * @see com.artipie.docker.asto.AstoDocker
 */
public interface Docker {

    /**
     * Docker repo by name.
     *
     * @param name Repository name
     * @return Repository object
     */
    Repo repo(String name);

    /**
     * Docker repositories catalog.
     *
     * @param pagination  Pagination parameters.
     * @return Catalog.
     */
    CompletableFuture<Catalog> catalog(Pagination pagination);
}
