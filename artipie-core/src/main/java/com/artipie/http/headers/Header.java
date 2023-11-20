/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP header.
 * Name of header is considered to be case-insensitive when compared to one another.
 *
 * @since 0.8
 */
public final class Header implements Map.Entry<String, String> {

    /**
     * Name.
     */
    private final String name;

    /**
     * Value.
     */
    private final String value;

    /**
     * Ctor.
     *
     * @param entry Entry representing a header.
     */
    public Header(final Map.Entry<String, String> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
     * Ctor.
     *
     * @param name Name.
     * @param value Value.
     */
    public Header(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.name;
    }

    @Override
    public String getValue() {
        return this.value.replaceAll("^\\s+", "");
    }

    @Override
    public String setValue(final String ignored) {
        throw new UnsupportedOperationException("Value cannot be modified");
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        final Header header = (Header) that;
        return this.lowercaseName().equals(header.lowercaseName())
            && this.getValue().equals(header.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lowercaseName(), this.getValue());
    }

    @Override
    public String toString() {
        return String.format("%s: %s", this.name, this.getValue());
    }

    /**
     * Converts name to lowercase for comparison.
     *
     * @return Name in lowercase.
     */
    private String lowercaseName() {
        return this.name.toLowerCase(Locale.US);
    }

    /**
     * Abstract decorator for Header.
     *
     * @since 0.9
     */
    public abstract static class Wrap implements Map.Entry<String, String> {

        /**
         * Origin header.
         */
        private final Map.Entry<String, String> header;

        /**
         * Ctor.
         *
         * @param header Header.
         */
        protected Wrap(final Map.Entry<String, String> header) {
            this.header = header;
        }

        @Override
        public final String getKey() {
            return this.header.getKey();
        }

        @Override
        public final String getValue() {
            return this.header.getValue();
        }

        @Override
        public final String setValue(final String value) {
            return this.header.setValue(value);
        }

        @Override
        @SuppressWarnings("PMD.OnlyOneReturn")
        public final boolean equals(final Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            final Wrap wrap = (Wrap) that;
            return Objects.equals(this.header, wrap.header);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(this.header);
        }

        @Override
        public final String toString() {
            return this.header.toString();
        }
    }
}
