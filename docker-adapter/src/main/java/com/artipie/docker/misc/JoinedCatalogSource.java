/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Source of catalog built by loading and merging multiple catalogs.
 *
 * @since 0.10
 */
public final class JoinedCatalogSource {

    /**
     * Dockers for reading.
     */
    private final List<Docker> dockers;

    private final Pagination pagination;

    /**
     * @param pagination Pagination parameters.
     * @param dockers Registries to load catalogs from.
     */
    public JoinedCatalogSource(Pagination pagination, Docker... dockers) {
        this(Arrays.asList(dockers), pagination);
    }

    /**
     * @param dockers Registries to load catalogs from.
     * @param pagination Pagination parameters.
     */
    public JoinedCatalogSource(List<Docker> dockers, Pagination pagination) {
        this.dockers = dockers;
        this.pagination = pagination;
    }

    /**
     * Load catalog.
     *
     * @return Catalog.
     */
    public CompletableFuture<Catalog> catalog() {
        final List<CompletionStage<List<String>>> all = this.dockers.stream().map(
            docker -> docker.catalog(pagination)
                .thenApply(ParsedCatalog::new)
                .thenCompose(ParsedCatalog::repos)
                .exceptionally(err -> Collections.emptyList())
        ).collect(Collectors.toList());
        return CompletableFuture.allOf(all.toArray(new CompletableFuture<?>[0]))
            .thenApply(nothing -> all.stream().flatMap(stage -> stage.toCompletableFuture().join().stream()).toList())
            .thenApply(names -> new CatalogPage(names, pagination));
    }
}
