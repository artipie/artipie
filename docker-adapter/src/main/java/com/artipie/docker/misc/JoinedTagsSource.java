/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.Manifests;
import com.artipie.docker.Tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Source of tags built by loading and merging multiple tag lists.
 */
public final class JoinedTagsSource {

    /**
     * Repository name.
     */
    private final String repo;

    /**
     * Manifests for reading.
     */
    private final List<Manifests> manifests;

    /**
     * @param repo Repository name.
     * @param pagination Pagination parameters.
     * @param manifests Sources to load tags from.
     */
    public JoinedTagsSource(String repo, Pagination pagination, Manifests... manifests) {
        this(repo, Arrays.asList(manifests), pagination);
    }

    private final Pagination pagination;

    /**
     * @param repo Repository name.
     * @param manifests Sources to load tags from.
     * @param pagination Pagination pagination.
     */
    public JoinedTagsSource(String repo, List<Manifests> manifests, Pagination pagination) {
        this.repo = repo;
        this.manifests = manifests;
        this.pagination = pagination;
    }

    /**
     * Load tags.
     *
     * @return Tags.
     */
    public CompletableFuture<Tags> tags() {
        CompletableFuture<List<String>>[] futs = new CompletableFuture[manifests.size()];
        for (int i = 0; i < manifests.size(); i++) {
            futs[i] = manifests.get(i).tags(pagination)
                .thenCompose(tags -> new ParsedTags(tags).tags())
                .toCompletableFuture()
                .exceptionally(err -> Collections.emptyList());
        }
        return CompletableFuture.allOf(futs)
            .thenApply(v -> {
                List<String> names = new ArrayList<>();
                Arrays.stream(futs).forEach(fut -> names.addAll(fut.join()));
                return new TagsPage(repo, names, pagination);
            });
    }
}
