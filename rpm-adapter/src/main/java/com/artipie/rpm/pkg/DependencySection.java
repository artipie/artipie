/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import org.redline_rpm.header.AbstractHeader;

/**
 * Rpm package dependency section.
 * <a href="https://rpm-software-management.github.io/rpm/manual/tags.html">Tags</a>.
 * <a href="https://rpm-software-management.github.io/rpm/manual/dependencies.html">Deps</a>.
 * @since 1.11
 */
public final class DependencySection {

    /**
     * Xml-section name.
     */
    private final String name;

    /**
     * Tag for names.
     */
    private final AbstractHeader.Tag names;

    /**
     * Tag for versions.
     */
    private final AbstractHeader.Tag versions;

    /**
     * Tag for flags.
     */
    private final AbstractHeader.Tag flags;

    /**
     * Ctor.
     * @param name Xml-section name
     * @param names Tag for names
     * @param versions Tag for versions
     * @param flags Tag for flags
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public DependencySection(
        final String name, final AbstractHeader.Tag names, final AbstractHeader.Tag versions,
        final AbstractHeader.Tag flags
    ) {
        this.name = name;
        this.names = names;
        this.versions = versions;
        this.flags = flags;
    }

    /**
     * Xml-section name.
     * @return String name for xml
     */
    public String xmlName() {
        return this.name;
    }

    /**
     * Tag for names.
     * @return Rpm tag to get deps names
     */
    public AbstractHeader.Tag tagForNames() {
        return this.names;
    }

    /**
     * Tag for versions.
     * @return Rpm tag to get deps versions
     */
    public AbstractHeader.Tag tagForVersions() {
        return this.versions;
    }

    /**
     * Tag for flags.
     * @return Rpm tag to get deps flags
     */
    public AbstractHeader.Tag tagForFlags() {
        return this.flags;
    }
}
