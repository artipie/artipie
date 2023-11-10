/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * File in directory.
 * @since 0.9
 */
public final class FileInDir {

    /**
     * Directory.
     */
    private final Path dir;

    /**
     * Ctor.
     * @param dir Directory
     */
    public FileInDir(final Path dir) {
        this.dir = dir;
    }

    /**
     * Searches for the file by subst in the directory.
     * @param substr File name substr
     * @return Path to the file
     * @throws IOException On Error
     * @throws IllegalArgumentException if not found
     */
    public Path find(final String substr) throws IOException {
        try (Stream<Path> files = Files.walk(this.dir)) {
            return files.filter(path -> path.getFileName().toString().contains(substr))
                .findFirst().orElseThrow(
                    () -> new IllegalArgumentException(
                        String.format(
                            "Metafile %s does not exists in %s", substr, this.dir.toString()
                        )
                    )
                );
        }
    }

}
