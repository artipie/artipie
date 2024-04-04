/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.perms;

import com.artipie.docker.http.CatalogSlice;
import com.artipie.security.perms.Action;

import java.util.Collections;
import java.util.Set;

/**
 * Registry permission categories.
 *
 * @since 0.18
 */
public enum RegistryCategory implements Action {

    /**
     * Base category, check {@link com.artipie.docker.http.BaseEntity}.
     */
    BASE("base", 0x4),

    /**
     * Catalog category, check {@link CatalogSlice}.
     */
    CATALOG("catalog", 0x2),

    /**
     * Any category.
     */
    ANY("*", 0x4 | 0x2);

    /**
     * The name of the category.
     */
    private final String name;

    /**
     * Category mask.
     */
    private final int mask;

    /**
     * @param name Category name
     * @param mask Category mask
     */
    RegistryCategory(final String name, final int mask) {
        this.name = name;
        this.mask = mask;
    }

    @Override
    public Set<String> names() {
        return Collections.singleton(this.name);
    }

    @Override
    public int mask() {
        return this.mask;
    }

    /**
     * Get category int mask by name.
     * @param name The category name
     * @return The mask
     * @throws IllegalArgumentException is the category not valid
     */
    public static int maskByCategory(String name) {
        for (Action item : values()) {
            if (item.names().contains(name)) {
                return item.mask();
            }
        }
        throw new IllegalArgumentException("Unknown permission action " + name);
    }
}
