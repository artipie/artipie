/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.rq.RequestLineFrom;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RegExp repository filter.
 *
 * Uses path part of request or full uri for matching.
 *
 * Yaml format:
 * <pre>
 *   filter: regular_expression
 *   priority: priority_value
 *   raw: true
 *   case_insensitive: true
 *
 *   where
 *     'filter' is mandatory and value contains regular expression for request matching.
 *     'priority_value' is optional and provides priority value. Default value is zero priority.
 *     'full_uri' is optional with default with value 'false'
 *       and implies to match with full uri or path part of uri.
 *     'case_insensitive' is optional with default value 'false'
 *       and implies to ignore case in regular expression matching.
 * </pre>
 *
 * @since 1.2
 */
public final class RegexpFilter extends Filter {
    /**
     * Path regexp pattern.
     */
    private final Pattern pattern;

    /**
     * Whether match full uri or path part of uri.
     */
    private final boolean fulluri;

    /**
     * Ctor.
     *
     * @param yaml Yaml mapping to read filters from
     */
    @SuppressWarnings(
        {"PMD.ConstructorOnlyInitializesOrCallOtherConstructors", "PMD.AvoidDuplicateLiterals"}
    )
    public RegexpFilter(final YamlMapping yaml) {
        super(yaml);
        this.fulluri = Boolean.parseBoolean(yaml.string("full_uri"));
        if (Boolean.parseBoolean(yaml.string("case_insensitive"))) {
            this.pattern = Pattern.compile(yaml.string("filter"), Pattern.CASE_INSENSITIVE);
        } else {
            this.pattern = Pattern.compile(yaml.string("filter"));
        }
    }

    @Override
    public boolean check(final RequestLineFrom line,
        final Iterable<Map.Entry<String, String>> headers) {
        final boolean res;
        if (this.fulluri) {
            res = this.pattern.matcher(line.uri().toString()).matches();
        } else {
            res = this.pattern.matcher(line.uri().getPath()).matches();
        }
        return res;
    }
}
