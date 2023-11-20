/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;

/**
 * Filter factory.
 *
 * @since 1.2
 */
public interface FilterFactory {
    /**
     * Instantiate filter.
     * @param yaml Yaml mapping to read filter from
     * @return Filter
     */
    Filter newFilter(YamlMapping yaml);
}
