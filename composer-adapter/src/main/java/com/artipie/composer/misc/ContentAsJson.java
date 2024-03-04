/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.misc;

import com.artipie.asto.Content;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.concurrent.CompletionStage;

/**
 * Auxiliary class for converting content to json.
 * @since 0.4
 */
public final class ContentAsJson {
    /**
     * Source content.
     */
    private final Content source;

    /**
     * Ctor.
     * @param content Source content
     */
    public ContentAsJson(final Content content) {
        this.source = content;
    }

    /**
     * Converts content to json.
     * @return JSON object
     */
    public CompletionStage<JsonObject> value() {
        return this.source
            .asStringFuture()
            .thenApply(
                str -> {
                    try (JsonReader reader = Json.createReader(new StringReader(str))) {
                        return reader.readObject();
                    }
                }
            );
    }
}
