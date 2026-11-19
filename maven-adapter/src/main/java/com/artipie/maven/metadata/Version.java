/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.metadata;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Artifact version.
 * @since 0.5
 */
public final class Version implements Comparable<Version> {

    /**
     * Version value as string.
     */
    private final ComparableVersion value;

    /**
     * Ctor.
     * @param value Version as string
     */
    public Version(final String value) {
        this.value = new ComparableVersion(value);
    }

    @Override
    public int compareTo(final Version another) {
        return this.value.compareTo(another.value);
    }

}
