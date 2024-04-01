/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.ResponseBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slice that filters content of repository.
 */
public class FilterSlice implements Slice {

    private final Slice origin;

    /**
     * Filter engine.
     */
    private final Filters filters;

    /**
     * @param origin Origin slice
     * @param yaml Yaml mapping to read filters from
     */
    public FilterSlice(final Slice origin, final YamlMapping yaml) {
        this(
            origin,
            Optional.of(yaml.yamlMapping("filters"))
                .map(Filters::new)
                .get()
        );
    }

    /**
     * @param origin Origin slice
     * @param filters Filters
     */
    public FilterSlice(final Slice origin, final Filters filters) {
        this.origin = origin;
        this.filters = Objects.requireNonNull(filters);
    }

    @Override
    public final CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        if (this.filters.allowed(line, headers)) {
            return this.origin.response(line, headers, body);
        }
        return CompletableFuture.completedFuture(ResponseBuilder.forbidden().build());
    }
}
