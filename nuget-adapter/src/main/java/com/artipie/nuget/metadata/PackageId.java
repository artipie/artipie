/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import java.util.Locale;

/**
 * Package id nuspec field.
 * See <a href="https://docs.microsoft.com/en-us/dotnet/api/system.string.tolowerinvariant?view=netstandard-2.0#System_String_ToLowerInvariant">.NET's System.String.ToLowerInvariant()</a>.
 * @since 0.6
 */
public final class PackageId implements NuspecField {

    /**
     * Raw value of package id tag.
     */
    private final String val;

    /**
     * Ctor.
     * @param val Raw value of package id tag
     */
    public PackageId(final String val) {
        this.val = val;
    }

    @Override
    public String raw() {
        return this.val;
    }

    @Override
    public String normalized() {
        return this.val.toLowerCase(Locale.getDefault());
    }

    @Override
    public String toString() {
        return this.val;
    }
}
