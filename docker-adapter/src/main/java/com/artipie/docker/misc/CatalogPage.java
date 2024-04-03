/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.util.Collection;

/**
 * {@link Catalog} that is a page of given repository names list.
 *
 * @since 0.10
 */
public final class CatalogPage implements Catalog {

    /**
     * Repository names.
     */
    private final Collection<RepoName> names;

    private final Pagination pagination;

    /**
     * @param names Repository names.
     * @param pagination Pagination parameters.
     */
    public CatalogPage(Collection<RepoName> names, Pagination pagination) {
        this.names = names;
        this.pagination = pagination;
    }

    @Override
    public Content json() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        this.names.stream()
            .filter(pagination::lessThan)
            .map(RepoName::value)
            .sorted()
            .distinct()
            .limit(pagination.limit())
            .forEach(builder::add);
        return new Content.From(
            Json.createObjectBuilder()
                .add("repositories", builder)
                .build()
                .toString()
                .getBytes()
        );
    }
}
