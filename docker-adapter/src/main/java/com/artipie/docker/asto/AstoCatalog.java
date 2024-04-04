/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.docker.Catalog;
import com.artipie.docker.misc.CatalogPage;
import com.artipie.docker.misc.Pagination;

import java.util.Collection;

/**
 * Asto implementation of {@link Catalog}. Catalog created from list of keys.
 *
 * @since 0.9
 */
final class AstoCatalog implements Catalog {

    /**
     * Repositories root key.
     */
    private final Key root;

    /**
     * List of keys inside repositories root.
     */
    private final Collection<Key> keys;
    private final Pagination pagination;

    /**
     * @param root Repositories root key.
     * @param keys List of keys inside repositories root.
     * @param pagination Pagination parameters.
     */
    AstoCatalog(Key root, Collection<Key> keys, Pagination pagination) {
        this.root = root;
        this.keys = keys;
        this.pagination = pagination;
    }

    @Override
    public Content json() {
        return new CatalogPage(this.repos(), this.pagination).json();
    }

    /**
     * Convert keys to ordered set of repository names.
     *
     * @return Ordered repository names.
     */
    private Collection<String> repos() {
        return new Children(this.root, this.keys)
            .names();
    }
}
