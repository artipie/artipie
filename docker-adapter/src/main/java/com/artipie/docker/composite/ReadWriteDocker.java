/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Read-write {@link Docker} implementation.
 * It delegates read operation to one {@link Docker} and writes {@link Docker} to another.
 * This class can be used to create virtual repository
 * by composing {@link com.artipie.docker.proxy.ProxyDocker}
 * and {@link com.artipie.docker.asto.AstoDocker}.
 *
 * @since 0.3
 */
public final class ReadWriteDocker implements Docker {

    /**
     * Docker for reading.
     */
    private final Docker read;

    /**
     * Docker for writing.
     */
    private final Docker write;

    /**
     * Ctor.
     *
     * @param read Docker for reading.
     * @param write Docker for writing.
     */
    public ReadWriteDocker(final Docker read, final Docker write) {
        this.read = read;
        this.write = write;
    }

    @Override
    public Repo repo(final RepoName name) {
        return new ReadWriteRepo(this.read.repo(name), this.write.repo(name));
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return this.read.catalog(from, limit);
    }
}
