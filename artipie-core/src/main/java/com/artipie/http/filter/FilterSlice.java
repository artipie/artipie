/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Slice that filters content of repository.
 * @since 1.2
 */
public class FilterSlice implements Slice {
    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Filter engine.
     */
    private final Filters filters;

    /**
     * Ctor.
     * @param origin Origin slice
     * @param yaml Yaml mapping to read filters from
     * @checkstyle HiddenFieldCheck (10 lines)
     */
    public FilterSlice(final Slice origin, final YamlMapping yaml) {
        this(
            origin,
            Optional.of(yaml.yamlMapping("filters"))
                .map(filters -> new Filters(filters))
                .get()
        );
    }

    /**
     * Ctor.
     * @param origin Origin slice
     * @param filters Filters
     */
    public FilterSlice(final Slice origin, final Filters filters) {
        this.origin = origin;
        this.filters = Objects.requireNonNull(filters);
    }

    @Override
    public final Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Response response;
        if (this.filters.allowed(line, headers)) {
            response = this.origin.response(line, headers, body);
        } else {
            response = new RsWithStatus(RsStatus.FORBIDDEN);
        }
        return response;
    }
}
