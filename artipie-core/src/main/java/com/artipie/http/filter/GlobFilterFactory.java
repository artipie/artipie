/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;

/**
 * Glob filter factory.
 *
 * @since 1.2
 */
@ArtipieFilterFactory("glob")
public final class GlobFilterFactory implements FilterFactory {
    @Override
    public Filter newFilter(final YamlMapping yaml) {
        return new GlobFilter(yaml);
    }
}
