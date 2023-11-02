/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

/**
 * Nuspec xml metadata field.
 * @since 0.6
 */
public interface NuspecField {

    /**
     * Original raw value (as it was in xml).
     * @return String value
     */
    String raw();

    /**
     * Normalized value of the field.
     * @return Normalized value
     */
    String normalized();

}
