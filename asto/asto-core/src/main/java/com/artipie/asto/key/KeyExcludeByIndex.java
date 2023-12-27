/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.LinkedList;
import java.util.List;

/**
 * Key that excludes a part by its index.
 * @implNote If index is out of bounds, the class can return the origin key.
 * @since 1.9.1
 */
public final class KeyExcludeByIndex extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param index Index of part
     */
    public KeyExcludeByIndex(final Key key, final int index) {
        super(
            new From(KeyExcludeByIndex.exclude(key, index))
        );
    }

    /**
     * Excludes part by its index.
     * @param key Key
     * @param index Index of part to exclude
     * @return List of parts
     */
    private static List<String> exclude(final Key key, final int index) {
        final List<String> parts = new LinkedList<>(key.parts());
        if (index >= 0 && index < parts.size()) {
            parts.remove(index);
        }
        return parts;
    }
}
