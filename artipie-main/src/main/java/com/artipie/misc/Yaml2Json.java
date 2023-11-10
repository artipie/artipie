/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.misc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonStructure;

/**
 * Transform yaml to json.
 * @since 0.1
 */
public final class Yaml2Json implements Function<String, JsonStructure> {

    @Override
    public JsonStructure apply(final String yaml) {
        try {
            return Json.createReader(
                new ByteArrayInputStream(
                    new ObjectMapper().writeValueAsBytes(
                        new ObjectMapper(new YAMLFactory())
                            .readValue(Yaml2Json.escapeAsterisk(yaml), Object.class)
                    )
                )
            ).read();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * EO yaml {@link com.amihaiemil.eoyaml} does always escapes * while
     * transforming {@link com.amihaiemil.eoyaml.YamlMapping} into string.
     * {@link com.fasterxml.jackson.dataformat.yaml.YAMLFactory} does not tolerate it,
     * so we have to escape.
     * Asterisk can be met in permissions as item of yaml sequence:
     * Jane:
     *   - *
     * which means that Jane is allowed to perform any actions.
     * And this "- *" is what we will escape.
     * @param yaml Yaml string
     * @return Yaml string with escaped asterisk
     */
    private static String escapeAsterisk(final String yaml) {
        return yaml.replace("- *", "- \"*\"");
    }
}
