/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.HeaderTags;
import java.util.Optional;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Rpm dependency: name, version and flag.
 * @since 1.2
 */
public final class RpmDependency {

    /**
     * Dependency name.
     */
    private final String name;

    /**
     * Dependency version.
     */
    private final HeaderTags.Version vers;

    /**
     * Dependency flag.
     */
    private final Optional<String> flag;

    /**
     * Ctor.
     * @param name Dependency name
     * @param vers Dependency version
     * @param flag Dependency flag
     */
    public RpmDependency(final String name, final HeaderTags.Version vers,
        final Optional<String> flag) {
        this.name = name;
        this.vers = vers;
        this.flag = flag;
    }

    /**
     * Is this dependency satisfied by another dependency? Yes, if:
     * 1) name and version are equal
     * 2) any version is absent/empty and names are equal
     * 3) names are equal and this dependency version and flag (GE or LE) satisfy version of the
     *    another dependency
     * 4) names are equal, flag is EQ, one of the `rel` version parts is absent
     * and versions are equal
     * @param aname Another dependency name
     * @param avers Another dependency version
     * @return True if this dependency can be satisfied by another
             */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public boolean isSatisfiedBy(final String aname, final HeaderTags.Version avers) {
        boolean res = false;
        if (this.name.concat(this.vers.toString()).equals(aname.concat(avers.toString()))) {
            res = true;
        } else if ((this.vers.toString().isEmpty() || avers.toString().isEmpty())
            && this.name.equals(aname)) {
            res = true;
        } else if (this.name.equals(aname) && this.flag.isPresent()
            && !this.flag.get().equals(HeaderTags.Flags.EQUAL.notation())) {
            // @checkstyle LineLengthCheck (5 lines)
            res = this.flag.get().equals(HeaderTags.Flags.LESS_OR_EQUAL.notation()) && avers.compareTo(this.vers) <= 0
                || this.flag.get().equals(HeaderTags.Flags.LESS.notation()) && avers.compareTo(this.vers) < 0
                || this.flag.get().equals(HeaderTags.Flags.GREATER_OR_EQUAL.notation()) && avers.compareTo(this.vers) >= 0
                || this.flag.get().equals(HeaderTags.Flags.GREATER.notation()) && avers.compareTo(this.vers) > 0;
        } else if (this.name.equals(aname) && this.flag.isPresent()
            && this.flag.get().equals(HeaderTags.Flags.EQUAL.notation())
            && (!this.vers.rel().isPresent() || !avers.rel().isPresent())) {
            res = new ComparableVersion(this.vers.ver())
                .compareTo(new ComparableVersion(avers.ver())) == 0;
        }
        return res;
    }
}
