/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.rq.RequestLineFrom;
import java.util.Map;
import java.util.Optional;

/**
 * Repository content filter.
 *
 * Yaml format:
 * <pre>
 *   priority: priority_value
 *
 *   where
 *     'priority_value' is optional and provides priority value. Default value is zero priority.
 * </pre>
 *
 * @since 1.2
 */
public abstract class Filter {
    /**
     * Priority yaml property name.
     */
    private static final String PRIORITY_KEY = "priority";

    /**
     * Default priority.
     */
    private static final int PRIORITY_DEFAULT = 0;

    /**
     * Priority.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final int priority;

    /**
     * Ctor.
     *
     * @param yaml Yaml mapping
     */
    public Filter(final YamlMapping yaml) {
        this.priority = Optional.ofNullable(yaml.string(Filter.PRIORITY_KEY))
            .map(Integer::parseInt)
            .orElse(Filter.PRIORITY_DEFAULT);
    }

    /**
     * Priority.
     * @return Priority
     */
    public int priority() {
        return this.priority;
    }

    /**
     * Checks conditions to get access to repository content.
     *
     * @param line Request line
     * @param headers Request headers.
     * @return True if request matched to access conditions.
     */
    public abstract boolean check(RequestLineFrom line,
        Iterable<Map.Entry<String, String>> headers);

    /**
     * Wrap is a decorative wrapper for Filter.
     *
     * @since 0.7
     */
    public abstract class Wrap extends Filter {
        /**
         * Origin filter.
         */
        private final Filter filter;

        /**
         * Ctor.
         *
         * @param filter Filter.
         * @param yaml Yaml mapping
         */
        public Wrap(final Filter filter, final YamlMapping yaml) {
            super(yaml);
            this.filter = filter;
        }

        @Override
        /**
         * Checks conditions to get access to repository content.
         *
         * @param line Request line
         * @param headers Request headers.
         * @return True if request matched to access conditions.
         */
        public boolean check(final RequestLineFrom line,
            final Iterable<Map.Entry<String, String>> headers) {
            return this.filter.check(line, headers);
        }
    }
}
