/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

/**
 * Glob repository filter.
 *<p>Uses path part of request for matching.
 *<p>Yaml format:
 * <pre>
 *   filter: expression
 *   priority: priority_value
 *
 *   where
 *     'filter' is mandatory and value contains globbing expression for request path matching.
 *     'priority_value' is optional and provides priority value. Default value is zero priority.
 * </pre>
 */
public final class GlobFilter extends Filter {

    private final PathMatcher matcher;

    /**
     * @param yaml Yaml mapping to read filters from
     */
    public GlobFilter(final YamlMapping yaml) {
        super(yaml);
        this.matcher = FileSystems.getDefault().getPathMatcher(
            String.format("glob:%s", yaml.string("filter"))
        );
    }

    @Override
    public boolean check(RequestLine line, Headers headers) {
        return this.matcher.matches(Paths.get(line.uri().getPath()));
    }
}
