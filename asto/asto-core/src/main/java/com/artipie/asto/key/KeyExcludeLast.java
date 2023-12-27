/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.LinkedList;
import java.util.List;

/**
 * Key that excludes the last occurrence of a part.
 * @implNote If part to exclude was not found, the class can return the origin key.
 * @since 1.9.1
 */
public final class KeyExcludeLast extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param part Part to exclude
     */
    public KeyExcludeLast(final Key key, final String part) {
        super(
            new From(KeyExcludeLast.exclude(key, part))
        );
    }

    /**
     * Excludes last occurrence of part.
     * @param key Key
     * @param part Part to exclude
     * @return List of parts
     */
    private static List<String> exclude(final Key key, final String part) {
        final List<String> allparts = key.parts();
        int ifound = -1;
        for (int ind = allparts.size() - 1; ind >= 0; ind = ind - 1) {
            final String prt = allparts.get(ind);
            if (prt.equals(part)) {
                ifound = ind;
                break;
            }
        }
        final List<String> parts = new LinkedList<>();
        for (int ind = 0; ind < allparts.size(); ind = ind + 1) {
            if (ind != ifound) {
                parts.add(allparts.get(ind));
            }
        }
        return parts;
    }

}
