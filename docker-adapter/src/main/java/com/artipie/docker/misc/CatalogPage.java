/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import java.util.Collection;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

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

    /**
     * From which name to start, exclusive.
     */
    private final Optional<RepoName> from;

    /**
     * Maximum number of names returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param names Repository names.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     */
    public CatalogPage(
        final Collection<RepoName> names,
        final Optional<RepoName> from,
        final int limit
    ) {
        this.names = names;
        this.from = from;
        this.limit = limit;
    }

    @Override
    public Content json() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        this.names.stream()
            .map(RepoName::value)
            .filter(name -> this.from.map(last -> name.compareTo(last.value()) > 0).orElse(true))
            .sorted()
            .distinct()
            .limit(this.limit)
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
