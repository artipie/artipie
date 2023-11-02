/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * Parsed {@link Tags} that is capable of extracting tags list and repository name
 * from origin {@link Tags}.
 *
 * @since 0.10
 */
public final class ParsedTags implements Tags {

    /**
     * Origin tags.
     */
    private final Tags origin;

    /**
     * Ctor.
     *
     * @param origin Origin tags.
     */
    public ParsedTags(final Tags origin) {
        this.origin = origin;
    }

    @Override
    public Content json() {
        return this.origin.json();
    }

    /**
     * Get repository name from origin.
     *
     * @return Repository name.
     */
    public CompletionStage<RepoName> repo() {
        return this.root().thenApply(root -> root.getString("name"))
            .thenApply(RepoName.Valid::new);
    }

    /**
     * Get tags list from origin.
     *
     * @return Tags list.
     */
    public CompletionStage<List<Tag>> tags() {
        return this.root().thenApply(root -> root.getJsonArray("tags")).thenApply(
            repos -> repos.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .map(Tag.Valid::new)
                .collect(Collectors.toList())
        );
    }

    /**
     * Read JSON root object from origin.
     *
     * @return JSON root.
     */
    private CompletionStage<JsonObject> root() {
        return new PublisherAs(this.origin.json()).bytes().thenApply(
            bytes -> Json.createReader(new ByteArrayInputStream(bytes)).readObject()
        );
    }
}
