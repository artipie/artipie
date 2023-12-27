/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Key;

/**
 * Last part of the storage {@link com.artipie.asto.Key}.
 * @since 0.24
 */
public final class KeyLastPart {

    /**
     * Origin key.
     */
    private final Key origin;

    /**
     * Ctor.
     * @param origin Key
     */
    public KeyLastPart(final Key origin) {
        this.origin = origin;
    }

    /**
     * Get last part of the key.
     * @return Key last part as string
     */
    public String get() {
        final String[] parts = this.origin.string().replaceAll("/$", "").split("/");
        return parts[parts.length - 1];
    }
}
