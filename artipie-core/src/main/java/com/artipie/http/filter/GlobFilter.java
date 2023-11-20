/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.rq.RequestLineFrom;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Glob repository filter.
 *
 * Uses path part of request for matching.
 *
 * Yaml format:
 * <pre>
 *   filter: expression
 *   priority: priority_value
 *
 *   where
 *     'filter' is mandatory and value contains globbing expression for request path matching.
 *     'priority_value' is optional and provides priority value. Default value is zero priority.
 * </pre>
 *
 * @since 1.2
 */
public final class GlobFilter extends Filter {
    /**
     * Path matcher.
     */
    private final PathMatcher matcher;

    /**
     * Ctor.
     *
     * @param yaml Yaml mapping to read filters from
     */
    public GlobFilter(final YamlMapping yaml) {
        super(yaml);
        this.matcher = FileSystems.getDefault().getPathMatcher(
            String.format("glob:%s", yaml.string("filter"))
        );
    }

    @Override
    public boolean check(final RequestLineFrom line,
        final Iterable<Map.Entry<String, String>> headers) {
        return this.matcher.matches(Paths.get(line.uri().getPath()));
    }
}
