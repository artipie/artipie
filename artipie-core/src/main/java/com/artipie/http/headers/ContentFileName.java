/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import java.net.URI;
import java.nio.file.Paths;

/**
 * Content-Disposition header for a file.
 *
 * @since 0.17.8
 */
public final class ContentFileName extends Header.Wrap {
    /**
     * Ctor.
     *
     * @param filename Name of attachment file.
     */
    public ContentFileName(final String filename) {
        super(
            new ContentDisposition(
                String.format("attachment; filename=\"%s\"", filename)
            )
        );
    }

    /**
     * Ctor.
     *
     * @param uri Requested URI.
     */
    public ContentFileName(final URI uri) {
        this(Paths.get(uri.getPath()).getFileName().toString());
    }
}
