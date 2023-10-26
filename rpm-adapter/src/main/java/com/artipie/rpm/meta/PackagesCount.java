/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Count of packages in metadata file.
 *
 * @since 0.11
 */
public class PackagesCount {

    /**
     * RegEx pattern for packages attribute.
     */
    private static final Pattern ATTR = Pattern.compile("packages=\"(\\d+)\"");

    /**
     * Max number of lines to read from file.
     */
    private static final int MAX_LINES = 10;

    /**
     * File path.
     */
    private final Path path;

    /**
     * Ctor.
     *
     * @param path File path.
     */
    public PackagesCount(final Path path) {
        this.path = path;
    }

    /**
     * Read packages count from `packages` attribute.
     *
     * @return Packages count.
     * @throws IOException In case I/O error occurred reading the file.
     */
    public int value() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(this.path)) {
            OptionalInt result = OptionalInt.empty();
            for (int lines = 0; lines < PackagesCount.MAX_LINES; lines = lines + 1) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final Matcher matcher = ATTR.matcher(line);
                if (matcher.find()) {
                    result = OptionalInt.of(Integer.parseInt(matcher.group(1)));
                    break;
                }
            }
            return result.orElseThrow(
                () -> new IllegalArgumentException("Failed to find packages attribute")
            );
        }
    }
}
