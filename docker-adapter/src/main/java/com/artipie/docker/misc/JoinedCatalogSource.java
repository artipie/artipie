/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    /**
     * From which name to start, exclusive.
     */
    private final Optional<RepoName> from;

    /**
     * Maximum number of names returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @param dockers Registries to load catalogs from.
     */
    public JoinedCatalogSource(
        final Optional<RepoName> from,
        final int limit,
        final Docker... dockers
    ) {
        this(Arrays.asList(dockers), from, limit);
    }

    /**
     * Ctor.
     *
     * @param dockers Registries to load catalogs from.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     */
    public JoinedCatalogSource(
        final List<Docker> dockers,
        final Optional<RepoName> from,
        final int limit
    ) {
        this.dockers = dockers;
        this.from = from;
        this.limit = limit;
    }

    /**
     * Load catalog.
     *
     * @return Catalog.
     */
    public CompletionStage<Catalog> catalog() {
        final List<CompletionStage<List<RepoName>>> all = this.dockers.stream().map(
            docker -> docker.catalog(this.from, this.limit)
                .thenApply(ParsedCatalog::new)
                .thenCompose(ParsedCatalog::repos)
                .exceptionally(err -> Collections.emptyList())
        ).collect(Collectors.toList());
        return CompletableFuture.allOf(all.toArray(new CompletableFuture<?>[0])).thenApply(
            nothing -> all.stream().flatMap(
                stage -> stage.toCompletableFuture().join().stream()
            ).collect(Collectors.toList())
        ).thenApply(names -> new CatalogPage(names, this.from, this.limit));
    }
}
