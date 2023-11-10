/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm.misc;

import com.artipie.asto.Content;
import java.nio.charset.StandardCharsets;

/**
 * Provides empty index file.
 * @since 0.3
 */
public final class EmptyIndex {
    /**
     * Content of index file.
     */
    private final String index;

    /**
     * Ctor.
     */
    public EmptyIndex() {
        this.index = String.format(
            "apiVersion: v1\ngenerated: %s\nentries:\n",
            new DateTimeNow().asString()
        );
    }

    /**
     * Index file as content.
     * @return Index file as content.
     */
    public Content asContent() {
        return new Content.From(this.index.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Index file as string.
     * @return Index file as string.
     */
    public String asString() {
        return this.index;
    }
}
