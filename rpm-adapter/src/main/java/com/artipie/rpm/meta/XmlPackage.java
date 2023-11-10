/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Xml metadata packages.
 * @since 0.9
 */
public enum XmlPackage {

    /**
     * Metadata primary.xml.
     */
    PRIMARY(
        "metadata",
        java.util.stream.Stream.of(
            new ImmutablePair<>("", "http://linux.duke.edu/metadata/common"),
            new ImmutablePair<>("rpm", "http://linux.duke.edu/metadata/rpm")
        ).collect(Collectors.toMap(Pair::getKey, Pair::getValue))
    ),

    /**
     * Metadata other.xml.
     */
    OTHER(
        "otherdata",
        Collections.singletonMap("", "http://linux.duke.edu/metadata/other")
    ),

    /**
     * Metadata filelists.xml.
     */
    FILELISTS(
        "filelists",
        Collections.singletonMap("", "http://linux.duke.edu/metadata/filelists")
    );

    /**
     * File suffix.
     * @checkstyle ConstantUsageCheck (5 lines)
     */
    private static final String SUFFIX = ".xml";

    /**
     * Tag name.
     */
    private final String tagname;

    /**
     * Metadata namespaces.
     */
    private final Map<String, String> namespaces;

    /**
     * Ctor.
     * @param tagname Tag name
     * @param namespaces Namespaces
     */
    XmlPackage(final String tagname, final Map<String, String> namespaces) {
        this.tagname = tagname;
        this.namespaces = namespaces;
    }

    /**
     * Metadata tag.
     * @return String tag
     */
    public String tag() {
        return this.tagname;
    }

    /**
     * Lower-case metadata name.
     * @return String lower-case name
     */
    public String lowercase() {
        return this.name().toLowerCase(Locale.getDefault());
    }

    /**
     * Metadata file prefix name.
     * @return String file prefix name.
     */
    public String tempPrefix() {
        return String.format("%s-", this.lowercase());
    }

    /**
     * Returns xml namespaces.
     * @return Map of the namespaces.
     */
    public Map<String, String> xmlNamespaces() {
        return this.namespaces;
    }

    /**
     * List of XmlPackage.
     * @since 0.10
     */
    public static final class Stream {

        /**
         * Need filelists?
         */
        private final boolean filelists;

        /**
         * Ctor.
         * @param filelists Need fileslist?
         */
        public Stream(final boolean filelists) {
            this.filelists = filelists;
        }

        /**
         * Stream of XmlPackage values.
         * @return Stream of XmlPackage
         */
        public java.util.stream.Stream<XmlPackage> get() {
            final java.util.stream.Stream<XmlPackage> res;
            if (this.filelists) {
                res = java.util.stream.Stream.of(values());
            } else {
                res = java.util.stream.Stream.of(PRIMARY, OTHER);
            }
            return res;
        }
    }

}
