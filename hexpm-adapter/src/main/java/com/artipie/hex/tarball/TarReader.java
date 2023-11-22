/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.ArtipieException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Allows to read content of named-entries from tar-source.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AssignmentInOperand")
public class TarReader {
    /**
     * File metadata.config.
     */
    public static final String CHECKSUM = "CHECKSUM";

    /**
     * File metadata.config.
     */
    public static final String METADATA = "metadata.config";

    /**
     * Portion size to read archive.
     */
    private static final int SIZE = 1024;

    /**
     * Tar archive as bytes.
     */
    private final byte[] bytes;

    /**
     * Ctor.
     * @param bytes Tar archive as bytes
     */
    public TarReader(final byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Reads content of entry stored in tar-archive.
     * @param name Name of entry
     * @return Optional of tar entry in byte array
     */
    public Optional<byte[]> readEntryContent(final String name) {
        byte[] content = null;
        try {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(this.bytes);
                TarArchiveInputStream tar = new TarArchiveInputStream(bis)
            ) {
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                    if (name.equals(entry.getName())) {
                        final ByteArrayOutputStream entrycontent = new ByteArrayOutputStream();
                        int len;
                        final byte[] buf = new byte[TarReader.SIZE];
                        while ((len = tar.read(buf)) != -1) {
                            entrycontent.write(buf, 0, len);
                        }
                        content = entrycontent.toByteArray();
                        break;
                    }
                }
            }
        } catch (final IOException ioex) {
            throw new ArtipieException(
                String.format("Cannot read content of '%s' from tar-archive", name),
                ioex
            );
        }
        return Optional.ofNullable(content);
    }
}
