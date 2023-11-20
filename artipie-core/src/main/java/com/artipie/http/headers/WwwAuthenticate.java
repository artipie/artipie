/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * WWW-Authenticate header.
 *
 * @since 0.12
 */
public final class WwwAuthenticate extends Header.Wrap {

    /**
     * Header name.
     */
    public static final String NAME = "WWW-Authenticate";

    /**
     * Header value RegEx.
     */
    private static final Pattern VALUE = Pattern.compile("(?<scheme>[^\"]*)( (?<params>.*))?");

    /**
     * Ctor.
     *
     * @param value Header value.
     */
    public WwwAuthenticate(final String value) {
        super(new Header(WwwAuthenticate.NAME, value));
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public WwwAuthenticate(final Headers headers) {
        this(new RqHeaders.Single(headers, WwwAuthenticate.NAME).asString());
    }

    /**
     * Get authorization scheme.
     *
     * @return Authorization scheme.
     */
    public String scheme() {
        return this.matcher().group("scheme");
    }

    /**
     * Get parameters list.
     *
     * @return Parameters list.
     */
    public List<Param> params() {
        return Optional.ofNullable(this.matcher().group("params")).map(
            params -> Stream.of(params.split(","))
                .map(Param::new)
                .collect(Collectors.toList())
        ).orElseGet(Collections::emptyList);
    }

    /**
     * Get realm parameter value.
     *
     * @return Realm parameter value.
     */
    public String realm() {
        return this.params().stream()
            .filter(param -> "realm".equals(param.name()))
            .map(Param::value)
            .findAny()
            .orElseThrow(
                () -> new IllegalStateException(
                    String.format("No realm param found: %s", this.getValue())
                )
            );
    }

    /**
     * Creates matcher for header value.
     *
     * @return Matcher for header value.
     */
    private Matcher matcher() {
        final String value = this.getValue();
        final Matcher matcher = VALUE.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                String.format("Failed to parse header value: %s", value)
            );
        }
        return matcher;
    }

    /**
     * WWW-Authenticate header parameter.
     *
     * @since 0.12
     */
    public static class Param {

        /**
         * Param RegEx.
         */
        private static final Pattern PATTERN = Pattern.compile(
            "(?<name>[^=]*)=\"(?<value>[^\"]*)\""
        );

        /**
         * Param raw string.
         */
        private final String string;

        /**
         * Ctor.
         *
         * @param string Param raw string.
         */
        public Param(final String string) {
            this.string = string;
        }

        /**
         * Param name.
         *
         * @return Name string.
         */
        public String name() {
            return this.matcher().group("name");
        }

        /**
         * Param value.
         *
         * @return Value string.
         */
        public String value() {
            return this.matcher().group("value");
        }

        /**
         * Creates matcher for param.
         *
         * @return Matcher for param.
         */
        private Matcher matcher() {
            final String value = this.string;
            final Matcher matcher = PATTERN.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                    String.format("Failed to parse param: %s", value)
                );
            }
            return matcher;
        }
    }
}
