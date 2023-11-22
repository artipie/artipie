/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Content;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import java.util.Collection;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * {@link Tags} that is a page of given tags list.
 *
 * @since 0.10
 */
public final class TagsPage implements Tags {

    /**
     * Repository name.
     */
    private final RepoName repo;

    /**
     * Tags.
     */
    private final Collection<Tag> tags;

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
     * @param repo Repository name.
     * @param tags Tags.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public TagsPage(
        final RepoName repo,
        final Collection<Tag> tags,
        final Optional<Tag> from,
        final int limit
    ) {
        this.repo = repo;
        this.tags = tags;
        this.from = from;
        this.limit = limit;
    }

    @Override
    public Content json() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        this.tags.stream()
            .map(Tag::value)
            .filter(name -> this.from.map(last -> name.compareTo(last.value()) > 0).orElse(true))
            .sorted()
            .distinct()
            .limit(this.limit)
            .forEach(builder::add);
        return new Content.From(
            Json.createObjectBuilder()
                .add("name", this.repo.value())
                .add("tags", builder)
                .build()
                .toString()
                .getBytes()
        );
    }
}
