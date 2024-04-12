/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Routing rule.
 * <p>
 * A rule which is applied to the request metadata such as request line and
 * headers. If rule matched, then routing slice {@link SliceRoute} will
 * redirect request to target {@link com.artipie.http.Slice}.
 */
public interface RtRule {

    /**
     * Fallback RtRule.
     */
    RtRule FALLBACK = (line, headers) -> true;

    /**
     * Apply this rule to request.
     * @param line Request line
     * @param headers Request headers
     * @return True if rule passed
     */
    boolean apply(RequestLine line, Headers headers);

    /**
     * This rule is matched only when all of the rules are matched.
     * This class is kept for backward compatibility reasons.
     * @since 0.5
     * @deprecated use {@link All} instead
     */
    @Deprecated
    final class Multiple extends All {

        /**
         * @param rules Rules array
         */
        public Multiple(final RtRule... rules) {
            super(Arrays.asList(rules));
        }

        /**
         * @param rules Rules
         */
        public Multiple(final Iterable<RtRule> rules) {
            super(rules);
        }
    }

    /**
     * This rule is matched only when all of the rules are matched.
     */
    class All implements RtRule {

        /**
         * Rules.
         */
        private final Iterable<RtRule> rules;

        /**
         * Route by multiple rules.
         * @param rules Rules array
         */
        public All(final RtRule... rules) {
            this(Arrays.asList(rules));
        }

        /**
         * Route by multiple rules.
         * @param rules Rules
         */
        public All(final Iterable<RtRule> rules) {
            this.rules = rules;
        }

        @Override
        public boolean apply(RequestLine line, Headers headers) {
            boolean match = true;
            for (final RtRule rule : this.rules) {
                if (!rule.apply(line, headers)) {
                    match = false;
                    break;
                }
            }
            return match;
        }
    }

    /**
     * This rule is matched only when any of the rules is matched.
     */
    final class Any implements RtRule {

        /**
         * Rules.
         */
        private final Iterable<RtRule> rules;

        /**
         * Route by any of the rules.
         * @param rules Rules array
         */
        public Any(final RtRule... rules) {
            this(Arrays.asList(rules));
        }

        /**
         * Route by any of the rules.
         * @param rules Rules
         */
        public Any(final Iterable<RtRule> rules) {
            this.rules = rules;
        }

        @Override
        public boolean apply(RequestLine line, Headers headers) {
            boolean match = false;
            for (final RtRule rule : this.rules) {
                if (rule.apply(line, headers)) {
                    match = true;
                    break;
                }
            }
            return match;
        }
    }

    /**
     * Route by path.
     */
    final class ByPath implements RtRule {

        /**
         * Request URI path pattern.
         */
        private final Pattern ptn;

        /**
         * By path rule.
         * @param ptn Path pattern string
         */
        public ByPath(final String ptn) {
            this(Pattern.compile(ptn));
        }

        /**
         * By path rule.
         * @param ptn Path pattern
         */
        public ByPath(final Pattern ptn) {
            this.ptn = ptn;
        }

        @Override
        public boolean apply(RequestLine line, Headers headers) {
            return this.ptn.matcher(line.uri().getPath()).matches();
        }
    }

    /**
     * Abstract decorator.
     * @since 0.16
     */
    abstract class Wrap implements RtRule {

        /**
         * Origin rule.
         */
        private final RtRule origin;

        /**
         * Ctor.
         * @param origin Rule
         */
        protected Wrap(final RtRule origin) {
            this.origin = origin;
        }

        @Override
        public final boolean apply(RequestLine line, Headers headers) {
            return this.origin.apply(line, headers);
        }
    }

    /**
     * Rule by header.
     * @since 0.17
     */
    final class ByHeader implements RtRule {

        /**
         * Header name.
         */
        private final String name;

        /**
         * Header value pattern.
         */
        private final Pattern ptn;

        /**
         * Ctor.
         * @param name Header name
         * @param ptn Header value pattern
         */
        public ByHeader(final String name, final Pattern ptn) {
            this.name = name;
            this.ptn = ptn;
        }

        /**
         * Ctor.
         * @param name Header name
         */
        public ByHeader(final String name) {
            this(name, Pattern.compile(".*"));
        }

        @Override
        public boolean apply(RequestLine line, Headers headers) {
            return new RqHeaders(headers, this.name).stream()
                .anyMatch(val -> this.ptn.matcher(val).matches());
        }
    }
}
