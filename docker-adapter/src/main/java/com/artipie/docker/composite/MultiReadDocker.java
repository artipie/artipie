/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.JoinedCatalogSource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Multi-read {@link Docker} implementation.
 * It delegates all read operations to multiple other {@link Docker} instances.
 * List of Docker instances is prioritized.
 * It means that if more then one of repositories contains an image for given name
 * then image from repository coming first is returned.
 * Write operations are not supported.
 * Might be used to join multiple proxy Dockers into single repository.
 *
 * @since 0.3
 */
public final class MultiReadDocker implements Docker {

    /**
     * Dockers for reading.
     */
    private final List<Docker> dockers;

    /**
     * Ctor.
     *
     * @param dockers Dockers for reading.
     */
    public MultiReadDocker(final Docker... dockers) {
        this(Arrays.asList(dockers));
    }

    /**
     * Ctor.
     *
     * @param dockers Dockers for reading.
     */
    public MultiReadDocker(final List<Docker> dockers) {
        this.dockers = dockers;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new MultiReadRepo(
            name,
            this.dockers.stream().map(docker -> docker.repo(name)).collect(Collectors.toList())
        );
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return new JoinedCatalogSource(this.dockers, from, limit).catalog();
    }
}
