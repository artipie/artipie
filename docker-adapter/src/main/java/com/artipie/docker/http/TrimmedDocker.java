/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Catalog;
import com.artipie.docker.Docker;
import com.artipie.docker.Repo;
import com.artipie.docker.misc.CatalogPage;
import com.artipie.docker.misc.ImageRepositoryName;
import com.artipie.docker.misc.Pagination;
import com.artipie.docker.misc.ParsedCatalog;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link Docker} to remove given prefix from repository names.
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
     * @param origin Docker origin
     * @param prefix Prefix to cut
     */
    public TrimmedDocker(Docker origin, String prefix) {
        this.origin = origin;
        this.prefix = prefix;
    }

    @Override
    public String registryName() {
        return origin.registryName();
    }

    @Override
    public Repo repo(String name) {
        return this.origin.repo(trim(name));
    }

    @Override
    public CompletableFuture<Catalog> catalog(Pagination pagination) {
        Pagination trimmed = new Pagination(
            trim(pagination.last()), pagination.limit()
        );
        return this.origin.catalog(trimmed)
            .thenCompose(catalog -> new ParsedCatalog(catalog).repos())
            .thenApply(names -> names.stream()
                .map(name -> String.format("%s/%s", this.prefix, name))
                .toList())
            .thenApply(names -> new CatalogPage(names, pagination));
    }

    /**
     * Trim prefix from start of original name.
     *
     * @param name Original name.
     * @return Name reminder.
     */
    private String trim(String name) {
        if (name != null) {
            final Pattern pattern = Pattern.compile(String.format("(?:%s)\\/(.+)", this.prefix));
            final Matcher matcher = pattern.matcher(name);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid image name: name `%s` must start with `%s/`",
                        name, this.prefix
                    )
                );
            }
            return ImageRepositoryName.validate(matcher.group(1));
        }
        return null;
    }
}
