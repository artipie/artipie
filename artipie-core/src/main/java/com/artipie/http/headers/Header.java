/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP header.
 * Name of header is considered to be case-insensitive when compared to one another.
 */
public class Header implements Map.Entry<String, String> {

    private final String name;
    private final String value;

    /**
     * @param entry Entry representing a header.
     */
    public Header(final Map.Entry<String, String> entry) {
        this(entry.getKey(), entry.getValue());
    }

    /**
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
    public boolean equals(final Object that) {
        if (this == that) {
            return true;
        }
        if(!(that instanceof Header header)){
            return false;
        }
        return this.lowercaseName().equals(header.lowercaseName())
            && this.lowercaseValue().equals(header.lowercaseValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lowercaseName(), this.lowercaseValue());
    }

    @Override
    public String toString() {
        return "Header{" +
            "name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
    }

    protected String lowercaseName() {
        return this.name.toLowerCase(Locale.US);
    }

    protected String lowercaseValue() {
        return this.getValue().toLowerCase(Locale.US);
    }
}
