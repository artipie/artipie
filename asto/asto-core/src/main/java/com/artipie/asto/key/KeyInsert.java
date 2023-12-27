/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.LinkedList;
import java.util.List;

/**
 * Key that inserts a part.
 *
 * @since 1.9.1
 */
public final class KeyInsert extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param part Part to insert
     * @param index Index of insertion
     */
    public KeyInsert(final Key key, final String part, final int index) {
        super(
            new From(KeyInsert.insert(key, part, index))
        );
    }

    /**
     * Inserts part.
     * @param key Key
     * @param part Part to insert
     * @param index Index of insertion
     * @return List of parts
     */
    private static List<String> insert(final Key key, final String part, final int index) {
        final List<String> parts = new LinkedList<>(key.parts());
        parts.add(index, part);
        return parts;
    }
}
