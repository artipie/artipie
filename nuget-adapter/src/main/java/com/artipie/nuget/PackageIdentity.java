/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Key;
import com.artipie.nuget.metadata.NuspecField;

/**
 * Package version identity.
 *
 * @since 0.1
 */
public final class PackageIdentity {

    /**
     * Package identity.
     */
    private final NuspecField id;

    /**
     * Package version.
     */
    private final NuspecField version;

    /**
     * Ctor.
     *
     * @param id Package identity.
     * @param version Package version.
     */
    public PackageIdentity(final NuspecField id, final NuspecField version) {
        this.id = id;
        this.version = version;
    }

    /**
     * Get key for .nupkg file.
     *
     * @return Key to .nupkg file.
     */
    public Key nupkgKey() {
        return new Key.From(
            this.rootKey(),
            String.format("%s.%s.nupkg", this.id.normalized(), this.version.normalized())
        );
    }

    /**
     * Get key for hash file.
     *
     * @return Key to hash file.
     */
    public Key hashKey() {
        return new Key.From(
            this.rootKey(),
            String.format("%s.%s.nupkg.sha512", this.id.normalized(), this.version.normalized())
        );
    }

    /**
     * Get key for .nuspec file.
     *
     * @return Key to .nuspec file.
     */
    public Key nuspecKey() {
        return new Key.From(this.rootKey(), String.format("%s.nuspec", this.id.normalized()));
    }

    /**
     * Get root key for package.
     *
     * @return Root key.
     */
    public Key rootKey() {
        return new Key.From(new PackageKeys(this.id).rootKey(), this.version.normalized());
    }

    @Override
    public String toString() {
        return String.format("Package: '%s' Version: '%s'", this.id, this.version);
    }
}
