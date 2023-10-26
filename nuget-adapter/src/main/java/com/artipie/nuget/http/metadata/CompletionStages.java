/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http.metadata;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collection of completion stages that all can be combined into single one.
 *
 * @param <T> Completion stages type.
 * @since 0.4
 */
final class CompletionStages<T> {

    /**
     * Completion stages.
     */
    private final Collection<CompletionStage<T>> stages;

    /**
     * Ctor.
     *
     * @param stages Completion stages.
     */
    CompletionStages(final Stream<CompletionStage<T>> stages) {
        this(stages.collect(Collectors.toList()));
    }

    /**
     * Ctor.
     *
     * @param stages Completion stages.
     */
    CompletionStages(final Collection<CompletionStage<T>> stages) {
        this.stages = stages;
    }

    /**
     * Combine original stages into single one that completes when all stages are complete.
     *
     * @return Combined completion stages.
     */
    public CompletionStage<Collection<T>> all() {
        final List<CompletableFuture<T>> futures = this.stages.stream()
            .map(CompletionStage::toCompletableFuture)
            .collect(Collectors.toList());
        return CompletableFuture.allOf(
            futures.stream().toArray(CompletableFuture[]::new)
        ).thenApply(
            nothing -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList())
        );
    }
}
