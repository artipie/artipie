/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.LinkedList;
import java.util.List;

/**
 * Key that excludes the first occurrence of a part.
 * @implNote If part to exclude was not found, the class can return the origin key.
 * @since 1.8.1
 */
public final class KeyExcludeFirst extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param part Part to exclude
     */
    public KeyExcludeFirst(final Key key, final String part) {
        super(
            new Key.From(KeyExcludeFirst.exclude(key, part))
        );
    }

    /**
     * Excludes first occurrence of part.
     * @param key Key
     * @param part Part to exclude
     * @return List of parts
     */
    private static List<String> exclude(final Key key, final String part) {
        final List<String> parts = new LinkedList<>();
        boolean isfound = false;
        for (final String prt : key.parts()) {
            if (prt.equals(part) && !isfound) {
                isfound = true;
                continue;
            }
            parts.add(prt);
        }
        return parts;
    }

}
