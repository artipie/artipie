/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Key;

/**
 * Name of package consisting of vendor name and package name "[vendor]/[package]".
 *
 * @since 0.1
 */
public final class Name {

    /**
     * Name string.
     */
    private final String value;

    /**
     * Ctor.
     *
     * @param value Name string.
     */
    public Name(final String value) {
        this.value = value;
    }

    /**
     * Generates key for package in store.
     *
     * @return Key for package in store.
     */
    public Key key() {
        return new Key.From(this.vendorPart(), String.format("%s.json", this.packagePart()));
    }

    /**
     * Generates name string value.
     *
     * @return Name string value.
     */
    public String string() {
        return this.value;
    }

    /**
     * Extracts vendor part from name.
     *
     * @return Vendor part of name.
     */
    private String vendorPart() {
        return this.part(0);
    }

    /**
     * Extracts package part from name.
     *
     * @return Package part of name.
     */
    private String packagePart() {
        return this.part(1);
    }

    /**
     * Extracts part of name by index.
     *
     * @param index Part index.
     * @return Part of name by index.
     */
    private String part(final int index) {
        final String[] parts = this.value.split("/");
        if (parts.length != 2) {
            throw new IllegalStateException(
                String.format(
                    "Invalid name. Should be like '[vendor]/[package]': '%s'",
                    this.value
                )
            );
        }
        return parts[index];
    }
}
