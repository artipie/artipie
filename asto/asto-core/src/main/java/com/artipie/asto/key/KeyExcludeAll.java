/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.key;

import com.artipie.asto.Key;
import java.util.stream.Collectors;

/**
 * Key that excludes all occurrences of a part.
 * @implNote If part to exclude was not found, the class can return the origin key.
 * @since 1.8.1
 */
public final class KeyExcludeAll extends Key.Wrap {

    /**
     * Ctor.
     * @param key Key
     * @param part Part to exclude
     */
    public KeyExcludeAll(final Key key, final String part) {
        super(
            new Key.From(
                key.parts().stream()
                    .filter(p -> !p.equals(part))
                    .collect(Collectors.toList())
            )
        );
    }
}
