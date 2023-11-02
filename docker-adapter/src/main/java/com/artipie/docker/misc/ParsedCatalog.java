/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonString;

/**
 * Parsed {@link Catalog} that is capable of extracting repository names list
 * from origin {@link Catalog}.
 *
 * @since 0.10
 */
public final class ParsedCatalog implements Catalog {

    /**
     * Origin catalog.
     */
    private final Catalog origin;

    /**
     * Ctor.
     *
     * @param origin Origin catalog.
     */
    public ParsedCatalog(final Catalog origin) {
        this.origin = origin;
    }

    @Override
    public Content json() {
        return this.origin.json();
    }

    /**
     * Get repository names list from origin catalog.
     *
     * @return Repository names list.
     */
    public CompletionStage<List<RepoName>> repos() {
        return new PublisherAs(this.origin.json()).bytes().thenApply(
            bytes -> Json.createReader(new ByteArrayInputStream(bytes)).readObject()
        ).thenApply(root -> root.getJsonArray("repositories")).thenApply(
            repos -> repos.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .map(RepoName.Valid::new)
                .collect(Collectors.toList())
        );
    }
}
