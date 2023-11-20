/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Util class for filters tests.
 *
 * @since 1.2
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class FiltersTestUtil {
    /**
     * Ctor.
     */
    private FiltersTestUtil() {
    }

    /**
     * Get request.
     * @param path Request path
     * @return Get request
     */
    public static String get(final String path) {
        return String.format("GET %s HTTP/1.1", path);
    }

    /**
     * Create yaml mapping from string.
     * @param yaml String containing yaml configuration
     * @return Yaml mapping
     */
    public static YamlMapping yaml(final String yaml) {
        try {
            return Yaml.createYamlInput(yaml).readYamlMapping();
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
