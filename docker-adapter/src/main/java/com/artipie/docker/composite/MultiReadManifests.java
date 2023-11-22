/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.misc.JoinedTagsSource;
import com.artipie.docker.ref.ManifestRef;
import com.jcabi.log.Logger;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Multi-read {@link Manifests} implementation.
 *
 * @since 0.3
 */
public final class MultiReadManifests implements Manifests {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Manifests for reading.
     */
    private final List<Manifests> manifests;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param manifests Manifests for reading.
     */
    public MultiReadManifests(final RepoName name, final List<Manifests> manifests) {
        this.name = name;
        this.manifests = manifests;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
        return firstNotEmpty(
            this.manifests.stream().map(
                mnfsts -> mnfsts.get(ref).handle(
                    (manifest, throwable) -> {
                        final CompletableFuture<Optional<Manifest>> result;
                        if (throwable == null) {
                            result = CompletableFuture.completedFuture(manifest);
                        } else {
                            Logger.error(
                                this, "Failed to read manifest %s: %[exception]s",
                                ref.string(),
                                throwable
                            );
                            result = CompletableFuture.completedFuture(Optional.empty());
                        }
                        return result;
                    }
                ).thenCompose(Function.identity())
            ).collect(Collectors.toList())
        );
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        return new JoinedTagsSource(this.name, this.manifests, from, limit).tags();
    }

    /**
     * Returns a new CompletionStage that is completed when first CompletionStage
     * from the list completes with non-empty result.
     * The result stage may be completed with empty value
     *
     * @param stages Completion stages.
     * @param <T> Result type.
     * @return Completion stage with first non-empty result.
     */
    private static <T> CompletionStage<Optional<T>> firstNotEmpty(
        final List<CompletionStage<Optional<T>>> stages
    ) {
        final CompletableFuture<Optional<T>> promise = new CompletableFuture<>();
        CompletionStage<Void> preceeding = CompletableFuture.allOf();
        for (final CompletionStage<Optional<T>> stage : stages) {
            preceeding = stage.thenCombine(
                preceeding,
                (opt, nothing) -> {
                    if (opt.isPresent()) {
                        promise.complete(opt);
                    }
                    return nothing;
                }
            );
        }
        preceeding.thenRun(() -> promise.complete(Optional.empty()));
        return promise;
    }
}
