/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.docker.Tags;
import com.artipie.docker.misc.Pagination;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import java.util.Collection;

/**
 * Asto implementation of {@link Tags}. Tags created from list of keys.
 *
 * @since 0.8
 */
final class AstoTags implements Tags {

    /**
     * Repository name.
     */
    private final String name;

    /**
     * Tags root key.
     */
    private final Key root;

    /**
     * List of keys inside tags root.
     */
    private final Collection<Key> keys;

    private final Pagination pagination;

    /**
     * @param name Repository name.
     * @param root Tags root key.
     * @param keys List of keys inside tags root.
     * @param pagination Pagination parameters.
     */
    AstoTags(String name, Key root, Collection<Key> keys, Pagination pagination) {
        this.name = name;
        this.root = root;
        this.keys = keys;
        this.pagination = pagination;
    }

    @Override
    public Content json() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        new Children(this.root, this.keys).names()
            .stream()
            .filter(pagination::lessThan)
            .limit(pagination.limit())
            .forEach(builder::add);
        return new Content.From(
            Json.createObjectBuilder()
                .add("name", this.name)
                .add("tags", builder)
                .build()
                .toString()
                .getBytes()
        );
    }
}
