/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * Convert json string to {@link YamlMapping}.
 * @since 0.26
 */
public final class Json2Yaml implements Function<String, YamlMapping> {

    @Override
    public YamlMapping apply(final String json) {
        try {
            return Yaml.createYamlInput(
                new YAMLMapper()
                    .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
                    .writeValueAsString(new ObjectMapper().readTree(json))
            ).readYamlMapping();
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}
