/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.Catalog;

import javax.json.Json;
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
    private final Collection<String> names;

    private final Pagination pagination;

    /**
     * @param names Repository names.
     * @param pagination Pagination parameters.
     */
    public CatalogPage(Collection<String> names, Pagination pagination) {
        this.names = names;
        this.pagination = pagination;
    }

    @Override
    public Content json() {
        return new Content.From(
            Json.createObjectBuilder()
                .add("repositories", pagination.apply(names.stream()))
                .build()
                .toString()
                .getBytes()
        );
    }
}
