/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.misc.JoinedCatalogSource;
import com.artipie.docker.misc.Pagination;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Multi-read {@link Docker} implementation.
 * It delegates all read operations to multiple other {@link Docker} instances.
 * List of Docker instances is prioritized.
 * It means that if more then one of repositories contains an image for given name
 * then image from repository coming first is returned.
 * Write operations are not supported.
 * Might be used to join multiple proxy Dockers into single repository.
 */
public final class MultiReadDocker implements Docker {


    /**
     * Dockers for reading.
     */
    private final List<Docker> dockers;

    /**
     * @param dockers Dockers for reading.
     */
    public MultiReadDocker(Docker... dockers) {
        this(Arrays.asList(dockers));
    }

    /**
     * Ctor.
     *
     * @param dockers Dockers for reading.
     */
    public MultiReadDocker(List<Docker> dockers) {
        this.dockers = dockers;
    }

    @Override
    public String registry() {
        return dockers.getFirst().registry();
    }

    @Override
    public Repo repo(String name) {
        return new MultiReadRepo(
            name,
            this.dockers.stream().map(docker -> docker.repo(name)).collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        return new JoinedCatalogSource(this.dockers, pagination).catalog();
    }
}
