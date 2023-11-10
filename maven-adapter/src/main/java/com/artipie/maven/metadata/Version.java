/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.vdurmont.semver4j.Semver;

/**
 * Artifact version.
 * @since 0.5
 */
public final class Version implements Comparable<Version> {

    /**
     * Version value as string.
     */
    private final String value;

    /**
     * Ctor.
     * @param value Version as string
     */
    public Version(final String value) {
        this.value = value;
    }

    @Override
    public int compareTo(final Version another) {
        return new Semver(this.value, Semver.SemverType.LOOSE)
            .compareTo(new Semver(another.value, Semver.SemverType.LOOSE));
    }

}
