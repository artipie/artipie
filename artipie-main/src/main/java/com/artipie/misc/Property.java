/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.misc;

import com.artipie.ArtipieException;
import java.util.Optional;

/**
 * Obtains value of property from properties which were already set in
 * the environment or in the file.
 * @since 0.23
 */
public final class Property {
    /**
     * Name of the property.
     */
    private final String name;

    /**
     * Ctor.
     * @param name Name of the property.
     */
    public Property(final String name) {
        this.name = name;
    }

    /**
     * Obtains long value of the property from already set properties or
     * from the file with values of the properties.
     * @param defval Default value for property
     * @return Long value of property or default value.
     * @throws ArtipieException In case of problem with parsing value of the property
     */
    public long asLongOrDefault(final long defval) {
        final long val;
        try {
            val = Long.parseLong(
                Optional.ofNullable(System.getProperty(this.name))
                    .orElse(
                        new ArtipieProperties().valueBy(this.name)
                            .orElse(String.valueOf(defval))
                    )
            );
        } catch (final NumberFormatException exc) {
            throw new ArtipieException(
                String.format("Failed to read property '%s'", this.name),
                exc
            );
        }
        return val;
    }
}
