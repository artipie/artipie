/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.repo;

import java.util.Collection;
import java.util.Optional;

/**
 * Artipie repositories registry.
 */
public interface Repositories {

    /**
     * Gets repository config by name.
     *
     * @param name Repository name
     * @return {@code Optional}, that contains repository configuration
     * or {@code Optional.empty()} if one is not found.
     */
    Optional<RepoConfig> config(String name);

    /**
     * Gets collection repositories configurations.
     *
     * @return Collection repository's configurations.
     */
    Collection<RepoConfig> configs();

    /**
     * Refreshes repositories configurations.
     */
    void refresh();
}
