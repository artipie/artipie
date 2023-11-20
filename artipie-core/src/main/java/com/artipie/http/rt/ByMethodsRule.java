/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Route by HTTP methods rule.
 * @since 0.16
 */
public final class ByMethodsRule implements RtRule {

    /**
     * Standard method rules.
     * @since 0.16
     */
    public enum Standard implements RtRule {
        /**
         * Rule for {@code GET} method.
         */
        GET(new ByMethodsRule(RqMethod.GET)),
        /**
         * Rule for {@code POST} method.
         */
        POST(new ByMethodsRule(RqMethod.POST)),
        /**
         * Rule for {@code PUT} method.
         */
        PUT(new ByMethodsRule(RqMethod.PUT)),
        /**
         * Rule for {@code DELETE} method.
         */
        DELETE(new ByMethodsRule(RqMethod.DELETE)),
        /**
         * All common read methods.
         */
        ALL_READ(new ByMethodsRule(RqMethod.GET, RqMethod.HEAD, RqMethod.OPTIONS)),
        /**
         * All common write methods.
         */
        ALL_WRITE(new ByMethodsRule(RqMethod.PUT, RqMethod.POST, RqMethod.DELETE, RqMethod.PATCH));

        /**
         * Origin rule.
         */
        private final RtRule origin;

        /**
         * Ctor.
         * @param origin Rule
         */
        Standard(final RtRule origin) {
            this.origin = origin;
        }

        @Override
        public boolean apply(final String line,
            final Iterable<Map.Entry<String, String>> headers) {
            return this.origin.apply(line, headers);
        }
    }

    /**
     * Method name.
     */
    private final Set<RqMethod> methods;

    /**
     * Route by methods.
     * @param methods Method names
     */
    public ByMethodsRule(final RqMethod... methods) {
        this(new HashSet<>(Arrays.asList(methods)));
    }

    /**
     * Route by methods.
     * @param methods Method names
     */
    public ByMethodsRule(final Set<RqMethod> methods) {
        this.methods = Collections.unmodifiableSet(methods);
    }

    @Override
    public boolean apply(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        return this.methods.contains(new RequestLineFrom(line).method());
    }
}
