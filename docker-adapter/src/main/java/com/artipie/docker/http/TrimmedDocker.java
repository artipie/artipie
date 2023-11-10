/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.CatalogPage;
import com.artipie.docker.misc.ParsedCatalog;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of {@link Docker} to remove given prefix from repository names.
 * @since 0.4
 */
public final class TrimmedDocker implements Docker {

    /**
     * Docker origin.
     */
    private final Docker origin;

    /**
     * Regex to cut prefix from repository name.
     */
    private final String prefix;

    /**
     * Ctor.
     * @param origin Docker origin
     * @param prefix Prefix to cut
     */
    public TrimmedDocker(final Docker origin, final String prefix) {
        this.origin = origin;
        this.prefix = prefix;
    }

    @Override
    public Repo repo(final RepoName name) {
        return this.origin.repo(this.trim(name));
    }

    @Override
    public CompletionStage<Catalog> catalog(final Optional<RepoName> from, final int limit) {
        return this.origin.catalog(from.map(this::trim), limit).thenCompose(
            catalog -> new ParsedCatalog(catalog).repos()
        ).thenApply(
            names -> names.stream()
                .map(name -> String.format("%s/%s", this.prefix, name.value()))
                .<RepoName>map(RepoName.Valid::new)
                .collect(Collectors.toList())
        ).thenApply(names -> new CatalogPage(names, from, limit));
    }

    /**
     * Trim prefix from start of original name.
     *
     * @param name Original name.
     * @return Name reminder.
     */
    private RepoName trim(final RepoName name) {
        final Pattern pattern = Pattern.compile(String.format("(?:%s)\\/(.+)", this.prefix));
        final Matcher matcher = pattern.matcher(name.value());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid image name: name `%s` must start with `%s/`",
                    name.value(), this.prefix
                )
            );
        }
        return new RepoName.Valid(matcher.group(1));
    }
}
