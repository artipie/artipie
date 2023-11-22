/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.npm.misc.DateTimeNowStr;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Bind {@code npm publish} generated json to an instance on {@link Meta}.
 *
 * @since 0.1
 */
final class NpmPublishJsonToMetaSkelethon {

    /**
     * The name json filed.
     */
    private static final String NAME = "name";

    /**
     * {@code npm publish} generated json to bind.
     */
    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param json The json to bind.
     */
    NpmPublishJsonToMetaSkelethon(final JsonObject json) {
        this.json = json;
    }

    /**
     * Bind the npm.
     * @return The skeleton for meta.json file
     */
    public JsonObject skeleton() {
        final String now = new DateTimeNowStr().value();
        final JsonObjectBuilder builder = Json.createObjectBuilder()
            .add(
                NpmPublishJsonToMetaSkelethon.NAME,
                this.json.getString(NpmPublishJsonToMetaSkelethon.NAME)
            )
            .add(
                "time",
                Json.createObjectBuilder()
                    .add("created", now)
                    .add("modified", now)
                    .build()
            )
            .add("users", Json.createObjectBuilder().build())
            .add("versions", Json.createObjectBuilder().build());
        this.addIfContains("_id", builder);
        this.addIfContains("readme", builder);
        return builder.build();
    }

    /**
     * Add key to builder if json contains this key.
     * @param key Key to add
     * @param builder Json builder
     */
    private void addIfContains(final String key, final JsonObjectBuilder builder) {
        if (this.json.containsKey(key)) {
            builder.add(key, this.json.getString(key));
        }
    }
}
