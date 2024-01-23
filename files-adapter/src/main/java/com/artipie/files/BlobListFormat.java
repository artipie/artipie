/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Key;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.json.Json;

/**
 * Format of a blob list.
 *
 * @since 0.8
 */
@FunctionalInterface
interface BlobListFormat {

    /**
     * Stamdard format implementations.
     * @since 1.0
         */
    enum Standard implements BlobListFormat {

        /**
         * Text format renders keys as a list of strings
         * separated by newline char {@code \n}.
         */
        TEXT(
            keys -> keys.stream().map(Key::string).collect(Collectors.joining("\n"))
        ),

        /**
         * Json format renders keys as JSON array with
         * keys items.
         */
        JSON(
            keys -> Json.createArrayBuilder(
                keys.stream().map(Key::string).collect(Collectors.toList())
            ).build().toString()
        ),

        /**
         * HTML format renders keys as simple markdown with ul, li and a tags.
         */
        HTML(
            keys -> String.format(
                String.join(
                    "\n",
                    "<!DOCTYPE html>",
                    "<html>",
                    "  <head><meta charset=\"utf-8\"/></head>",
                    "  <body>",
                    "    <ul>",
                    "%s",
                    "    </ul>",
                    "  </body>",
                    "</html>"
                ),
                keys.stream().map(
                    key -> String.format(
                        "      <li><a href=\"/%s\">%s</a></li>",
                        key.string(),
                        key.string()
                    )
                ).collect(Collectors.joining("\n"))
            )
        );

        /**
         * Format.
         */
        private final BlobListFormat fmt;

        /**
         * Enum instance.
         * @param fmt Format
         */
        Standard(final BlobListFormat fmt) {
            this.fmt = fmt;
        }

        @Override
        public String apply(final Collection<? extends Key> blobs) {
            return this.fmt.apply(blobs);
        }
    }

    /**
     * Apply the format to the list of blobs.
     * @param blobs List of blobs
     * @return Text formatted
     */
    String apply(Collection<? extends Key> blobs);
}
