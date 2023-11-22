/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.ArtipieException;
import com.artipie.http.rq.RequestLineFrom;
import java.util.regex.Pattern;

/**
 * Get package name (can be scoped) from request url.
 * @since 0.6
 */
public class PackageNameFromUrl {

    /**
     * Request url.
     */
    private final String url;

    /**
     * Ctor.
     * @param url Request url
     */
    public PackageNameFromUrl(final String url) {
        this.url = url;
    }

    /**
     * Gets package name from url.
     * @return Package name
     */
    public String value() {
        final String abspath = new RequestLineFrom(this.url).uri().getPath();
        final String context = "/";
        if (abspath.startsWith(context)) {
            return abspath.replaceFirst(
                String.format("%s/?", Pattern.quote(context)),
                ""
            );
        } else {
            throw new ArtipieException(
                new IllegalArgumentException(
                    String.format(
                        "Path is expected to start with '%s' but was '%s'",
                        context,
                        abspath
                    )
                )
            );
        }
    }
}
