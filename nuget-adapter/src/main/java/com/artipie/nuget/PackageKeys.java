/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Key;
import com.artipie.nuget.metadata.NuspecField;
import com.artipie.nuget.metadata.PackageId;

/**
 * Package identifier.
 *
 * @since 0.1
 */
public final class PackageKeys {

    /**
     * Package identifier string.
     */
    private final NuspecField raw;

    /**
     * Ctor.
     * @param id Package id
     */
    public PackageKeys(final NuspecField id) {
        this.raw = id;
    }

    /**
     * Ctor.
     *
     * @param raw Raw package identifier string.
     */
    public PackageKeys(final String raw) {
        this(new PackageId(raw));
    }

    /**
     * Get key for package root.
     *
     * @return Key for package root.
     */
    public Key rootKey() {
        return new Key.From(this.raw.normalized());
    }

    /**
     * Get key for package versions registry.
     *
     * @return Get key for package versions registry.
     */
    public Key versionsKey() {
        return new Key.From(this.rootKey(), "index.json");
    }

    @Override
    public String toString() {
        return this.raw.raw();
    }
}
