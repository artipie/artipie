/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;

/**
 * Key from path.
 * @since 0.6
 */
public final class KeyFromPath extends Key.Wrap {

    /**
     * Key from path string.
     * @param path Path string
     */
    public KeyFromPath(final String path) {
        super(new From(normalize(path)));
    }

    /**
     * Normalize path to use as a valid {@link Key}.
     * Removes leading slash char if exist.
     * @param path Path string
     * @return Normalized path
     */
    private static String normalize(final String path) {
        final String res;
        if (path.length() > 0 && path.charAt(0) == '/') {
            res = path.substring(1);
        } else {
            res = path;
        }
        return res;
    }
}
