/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.nuget.metadata.NuspecField;
import com.artipie.nuget.metadata.Version;
import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * NuGet package version enumeration.
 *
 * @since 0.1
 */
public final class Versions {

    /**
     * Name of array in JSON containing versions.
     */
    private static final String ARRAY = "versions";

    /**
     * Packages registry content.
     */
    private final JsonObject content;

    /**
     * Ctor.
     */
    public Versions() {
        this(
            Json.createObjectBuilder()
                .add(Versions.ARRAY, Json.createArrayBuilder())
                .build()
        );
    }

    /**
     * Ctor.
     *
     * @param content Packages registry content.
     */
    public Versions(final JsonObject content) {
        this.content = content;
    }

    /**
     * Add version.
     *
     * @param version Version.
     * @return Updated versions.
     */
    public Versions add(final NuspecField version) {
        final JsonArray versions = this.content.getJsonArray(Versions.ARRAY);
        final JsonArrayBuilder builder;
        if (versions == null) {
            builder = Json.createArrayBuilder();
        } else {
            builder = Json.createArrayBuilder(versions);
        }
        builder.add(version.normalized());
        return new Versions(
            Json.createObjectBuilder(this.content)
                .add(Versions.ARRAY, builder)
                .build()
        );
    }

    /**
     * Read all package versions.
     *
     * @return All versions sorted by natural order.
     */
    public List<NuspecField> all() {
        return this.content
            .getJsonArray(Versions.ARRAY)
            .getValuesAs(JsonString.class)
            .stream()
            .map(JsonString::getString)
            .map(Version::new)
            .sorted()
            .collect(ImmutableList.toImmutableList());
    }

    /**
     * Saves binary content to storage.
     *
     * @param storage Storage to use for saving.
     * @param key Key to store data at.
     * @return Completion of save operation.
     */
    public CompletableFuture<Void> save(final Storage storage, final Key key) {
        return storage.save(
            key,
            new Content.From(this.content.toString().getBytes(StandardCharsets.UTF_8))
        );
    }

}
