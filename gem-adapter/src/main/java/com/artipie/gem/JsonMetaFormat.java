/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.gem.GemMeta.MetaFormat;
import com.artipie.gem.GemMeta.MetaInfo;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 * New JSON format for Gem meta info.
 * @since 1.0
 */
public final class JsonMetaFormat implements MetaFormat {

    /**
     * JSON builder.
     */
    private final JsonObjectBuilder builder;

    /**
     * New JSON format.
     * @param builder JSON builder
     */
    public JsonMetaFormat(final JsonObjectBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void print(final String name, final String value) {
        this.builder.add(name, value);
    }

    @Override
    public void print(final String name, final MetaInfo value) {
        final JsonObjectBuilder child = Json.createObjectBuilder();
        value.print(new JsonMetaFormat(child));
        this.builder.add(name, child);
    }

    @Override
    public void print(final String name, final String[] values) {
        final JsonArrayBuilder arb = Json.createArrayBuilder();
        for (final String item : values) {
            arb.add(item);
        }
        this.builder.add(name, arb);
    }
}
