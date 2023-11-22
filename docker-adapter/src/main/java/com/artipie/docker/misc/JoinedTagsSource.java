/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Source of tags built by loading and merging multiple tag lists.
 *
 * @since 0.10
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
     * @checkstyle ParameterNumberCheck (2 lines)
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
     * @checkstyle ParameterNumberCheck (2 lines)
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
    public CompletionStage<Tags> tags() {
        final List<CompletionStage<List<Tag>>> all = this.manifests.stream().map(
            mnfsts -> mnfsts.tags(this.from, this.limit)
                .thenApply(ParsedTags::new)
                .thenCompose(ParsedTags::tags)
                .exceptionally(err -> Collections.emptyList())
        ).collect(Collectors.toList());
        return CompletableFuture.allOf(all.toArray(new CompletableFuture<?>[0])).thenApply(
            nothing -> all.stream().flatMap(
                stage -> stage.toCompletableFuture().join().stream()
            ).collect(Collectors.toList())
        ).thenApply(names -> new TagsPage(this.repo, names, this.from, this.limit));
    }
}
