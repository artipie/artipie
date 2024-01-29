/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

/**
 * Names of the optional fields of nuspes file.
 * Check <a href="https://learn.microsoft.com/en-us/nuget/reference/nuspec">docs</a> for more info.
 * @since 0.7
 */
public enum OptFieldName {

    TITLE("title"),

    SUMMARY("summary"),

    ICON("icon"),

    ICON_URL("iconUrl"),

    LICENSE("license"),

    LICENSE_URL("licenseUrl"),

    REQUIRE_LICENSE_ACCEPTANCE("requireLicenseAcceptance"),

    TAGS("tags"),

    PROJECT_URL("projectUrl"),

    RELEASE_NOTES("releaseNotes");

    /**
     * Xml field name.
     */
    private final String name;

    /**
     * Ctor.
     * @param name Xml field name
     */
    OptFieldName(final String name) {
        this.name = name;
    }

    /**
     * Get xml field name.
     * @return String xml name
     */
    public String get() {
        return this.name;
    }
}
