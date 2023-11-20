/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.json;

import java.io.StringReader;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonValue;

/**
 * Abstract package content representation that supports JSON transformation.
 *
 * @since 0.1
 */
public abstract class TransformedContent {
    /**
     * Original package content.
     */
    private final String data;

    /**
     * Ctor.
     * @param data Package content to be transformed
     */
    public TransformedContent(final String data) {
        this.data = data;
    }

    /**
     * Returns transformed package content as String.
     * @return Transformed package content
     */
    public JsonObject value() {
        return this.transformAssetRefs();
    }

    /**
     * Transforms asset references.
     * @param ref Original asset reference
     * @return Transformed asset reference
     */
    abstract String transformRef(String ref);

    /**
     * Transforms package JSON.
     * @return Transformed JSON
     */
    private JsonObject transformAssetRefs() {
        final JsonObject json = Json.createReader(new StringReader(this.data)).readObject();
        final JsonValue node = json.get("versions");
        final JsonPatchBuilder patch = Json.createPatchBuilder();
        if (node != null) {
            final Set<String> vrsns = node.asJsonObject().keySet();
            for (final String vers : vrsns) {
                final String path = String.format("/versions/%s/dist/tarball", vers);
                final String asset = node.asJsonObject()
                    .getJsonObject(vers)
                    .getJsonObject("dist")
                    .getString("tarball");
                patch.replace(path, this.transformRef(asset));
            }
        }
        return patch.build().apply(json);
    }
}
