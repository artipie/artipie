/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.amihaiemil.eoyaml.YamlMapping;

/**
 * Authentication factory creates auth instance from yaml settings.
 * Yaml settings is
 * <a href="https://github.com/artipie/artipie/wiki/Configuration">artipie main config</a>.
 * @since 1.3
 */
public interface AuthFactory {

    /**
     * Construct auth instance.
     * @param conf Yaml configuration
     * @return Instance of {@link Authentication}
     */
    Authentication getAuthentication(YamlMapping conf);

}
