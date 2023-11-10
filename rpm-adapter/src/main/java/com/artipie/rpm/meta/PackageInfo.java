/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.HeaderTags;
import java.util.Objects;

/**
 * Package info. Package name is constructed from name and architecture,
 * joined with "_": name_arch.
 *
 * @since 1.11
 */
public final class PackageInfo {

    /**
     * Delimiter for name and arch.
     */
    private static final String DELIMITER = "_";

    /**
     * Package name.
     */
    private final String pname;

    /**
     * Package version.
     */
    private final String vers;

    /**
     * Size.
     */
    private final long size;

    /**
     * Ctor.
     *
     * @param pname Package name
     * @param vers Package version
     * @param size Package size
     */
    public PackageInfo(final String pname, final String vers, final long size) {
        this.pname = pname;
        this.vers = vers;
        this.size = size;
    }

    /**
     * Ctor.
     * @param name Package name
     * @param arch Package arch
     * @param version Package version
     */
    public PackageInfo(final String name, final String arch, final String version) {
        this(String.join(PackageInfo.DELIMITER, name, arch), version, 0);
    }

    /**
     * Creates package info item from {@link HeaderTags}.
     *
     * @param tags Package tags meta info
     * @param size Package size
     */
    public PackageInfo(final HeaderTags tags, final long size) {
        this(String.join(PackageInfo.DELIMITER, tags.name(), tags.arch()), tags.version(), size);
    }

    /**
     * Package name.
     * @return The name of the package
     */
    public String name() {
        return this.pname;
    }

    /**
     * Package version.
     * @return Package version
     */
    public String version() {
        return this.vers;
    }

    /**
     * Package size.
     * @return The size in bytes
     */
    public long packageSize() {
        return this.size;
    }

    @Override
    public boolean equals(final Object other) {
        final boolean res;
        if (this == other) {
            res = true;
        } else if (other == null || getClass() != other.getClass()) {
            res = false;
        } else {
            final PackageInfo that = (PackageInfo) other;
            res = this.size == that.size
                && Objects.equals(this.pname, that.pname)
                && Objects.equals(this.vers, that.vers);
        }
        return res;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pname, this.vers, this.size);
    }
}
