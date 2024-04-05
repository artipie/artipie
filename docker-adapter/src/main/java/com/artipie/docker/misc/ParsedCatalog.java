/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;

import javax.json.Json;
import javax.json.JsonString;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Parsed {@link Catalog} that is capable of extracting repository names list
 * from origin {@link Catalog}.
 */
public final class ParsedCatalog implements Catalog {

    /**
     * Origin catalog.
     */
    private final Catalog origin;

    /**
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
    public CompletionStage<List<String>> repos() {
        return this.origin.json().asBytesFuture().thenApply(
            bytes -> Json.createReader(new ByteArrayInputStream(bytes)).readObject()
            ).thenApply(root -> root.getJsonArray("repositories"))
            .thenApply(
                repos -> repos.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .map(ImageRepositoryName::validate)
                .toList()
        );
    }
}
