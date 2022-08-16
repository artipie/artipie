/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.settings.repo.RepoConfig;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories registry.
 *
 * @since 0.13
 */
public interface Repositories {

    /**
     * Find repository config by name.
     *
     * @param name Repository name
     * @return Repository config
     */
    CompletionStage<RepoConfig> config(String name);
}
