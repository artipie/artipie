/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * Asto implementation of {@link Tags}. Tags created from list of keys.
 *
 * @since 0.8
 */
final class AstoTags implements Tags {

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Tags root key.
     */
    private final Key root;

    /**
     * List of keys inside tags root.
     */
    private final Collection<Key> keys;

    /**
     * From which tag to start, exclusive.
     */
    private final Optional<Tag> from;

    /**
     * Maximum number of tags returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param name Repository name.
     * @param root Tags root key.
     * @param keys List of keys inside tags root.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
         */
    AstoTags(
        final RepoName name,
        final Key root,
        final Collection<Key> keys,
        final Optional<Tag> from,
        final int limit
    ) {
        this.name = name;
        this.root = root;
        this.keys = keys;
        this.from = from;
        this.limit = limit;
    }

    @Override
    public Content json() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        this.tags().stream()
            .map(Tag::value)
            .filter(tag -> this.from.map(last -> tag.compareTo(last.value()) > 0).orElse(true))
            .limit(this.limit)
            .forEach(builder::add);
        return new Content.From(
            Json.createObjectBuilder()
                .add("name", this.name.value())
                .add("tags", builder)
                .build()
                .toString()
                .getBytes()
        );
    }

    /**
     * Convert keys to ordered set of tags.
     *
     * @return Ordered tags.
     */
    private Collection<Tag> tags() {
        return new Children(this.root, this.keys).names().stream()
            .map(Tag.Valid::new)
            .collect(Collectors.toList());
    }
}
