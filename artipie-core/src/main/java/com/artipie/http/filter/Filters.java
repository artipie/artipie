/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.http.rq.RequestLine;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filters.
 *<p>
 * Yaml format:
 * <pre>
 *   include: yaml-sequence of including filters
 *   exclude: yaml-sequence of excluding filters
 * </pre>
 */
public final class Filters {
    /**
     * Filter factory loader.
     */
    private static final FilterFactoryLoader FILTER_LOADER = new FilterFactoryLoader();

    /**
     * Including filters.
     */
    private final List<Filter> includes;

    /**
     * Excluding filters.
     */
    private final List<Filter> excludes;

    /**
     * @param yaml Yaml mapping to read filters from
     */
    public Filters(final YamlMapping yaml) {
        this.includes = Filters.readFilterList(yaml, "include");
        this.excludes = Filters.readFilterList(yaml, "exclude");
    }

    /**
     * Whether allowed to get access to repository content.
     * @param line Request line
     * @param headers Request headers.
     * @return True if is allowed to get access to repository content.
     */
    public boolean allowed(RequestLine line, Iterable<Map.Entry<String, String>> headers) {
        final boolean included = this.includes.stream()
            .anyMatch(filter -> filter.check(line, headers));
        final boolean excluded = this.excludes.stream()
            .anyMatch(filter -> filter.check(line, headers));
        return included & !excluded;
    }

    /**
     * Total number of filters.
     * @return Number of filters.
     */
    public int size() {
        return this.includes.size() + this.excludes.size();
    }

    /**
     * Reads yaml definitions of filters.
     * @param yaml Yaml-mapping
     * @param property Property name
     * @return List of filters ordered by descending priority
     */
    private static List<Filter> readFilterList(final YamlMapping yaml, final String property) {
        final List<Filter> list = Optional.ofNullable(yaml.yamlMapping(property))
            .map(
                map ->
                    map.keys().stream().map(key -> key.asScalar().value())
                        .map(
                            type -> map.yamlSequence(type).values().stream()
                                .map(YamlNode::asMapping)
                                .map(node -> Filters.FILTER_LOADER.newObject(type, node))
                                .collect(Collectors.toList())
                        )
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
            )
            .orElse(Collections.emptyList());
        list.sort(Collections.reverseOrder(Comparator.comparingInt(Filter::priority)));
        return list;
    }
}
