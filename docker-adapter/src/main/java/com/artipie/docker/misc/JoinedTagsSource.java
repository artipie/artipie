/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Source of tags built by loading and merging multiple tag lists.
 */
public final class JoinedTagsSource {

    /**
     * Repository name.
     */
    private final RepoName repo;

    /**
     * Manifests for reading.
     */
    private final List<Manifests> manifests;

    /**
     * From which tag to start, exclusive.
     */
    private final Optional<Tag> from;

    /**
     * Maximum number of tags returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param repo Repository name.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @param manifests Sources to load tags from.
     */
    public JoinedTagsSource(
        final RepoName repo,
        final Optional<Tag> from,
        final int limit,
        final Manifests... manifests
    ) {
        this(repo, Arrays.asList(manifests), from, limit);
    }

    /**
     * Ctor.
     *
     * @param repo Repository name.
     * @param manifests Sources to load tags from.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     */
    public JoinedTagsSource(
        final RepoName repo,
        final List<Manifests> manifests,
        final Optional<Tag> from,
        final int limit
    ) {
        this.repo = repo;
        this.manifests = manifests;
        this.from = from;
        this.limit = limit;
    }

    /**
     * Load tags.
     *
     * @return Tags.
     */
    public CompletableFuture<Tags> tags() {
        CompletableFuture<List<String>>[] futs = new CompletableFuture[manifests.size()];
        for (int i = 0; i < manifests.size(); i++) {
            futs[i] = manifests.get(i).tags(from, limit)
                .thenCompose(tags -> new ParsedTags(tags).tags())
                .toCompletableFuture()
                .exceptionally(err -> Collections.emptyList());
        }
        return CompletableFuture.allOf(futs)
            .thenApply(v -> {
                List<String> names = new ArrayList<>();
                Arrays.stream(futs).forEach(fut -> names.addAll(fut.join()));
                return new TagsPage(repo, names, from, limit);
            });
    }
}
