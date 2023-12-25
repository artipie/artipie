/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.ArtipieException;

/**
 * Helper object to read common metadata from {@link Meta}.
 * @since 1.11
 */
public final class MetaCommon {

    /**
     * Metadata.
     */
    private final Meta meta;

    /**
     * Ctor.
     * @param meta Metadata
     */
    public MetaCommon(final Meta meta) {
        this.meta = meta;
    }

    /**
     * Size.
     * @return Size
     */
    public long size() {
        return this.meta.read(Meta.OP_SIZE).orElseThrow(
            () -> new ArtipieException("SIZE couldn't be read")
        ).longValue();
    }
}
