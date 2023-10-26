/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.files;

import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Gzip.
 * @since 0.8
 */
public final class Gzip {

    /**
     * Path to gzip.
     */
    private final Path file;

    /**
     * Ctor.
     * @param file Path
     */
    public Gzip(final Path file) {
        this.file = file;
    }

    /**
     * Unpacks tar gzip to the temp dir.
     * @param dest Destination directory
     * @throws IOException If fails
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public void unpackTar(final Path dest) throws IOException {
        final GzipCompressorInputStream input =
            new GzipCompressorInputStream(Files.newInputStream(this.file));
        try (TarArchiveInputStream tar = new TarArchiveInputStream(input)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                final Path next = dest.resolve(entry.getName());
                if (!next.normalize().startsWith(dest)) {
                    throw new IllegalStateException("Bad tar.gz entry");
                }
                if (entry.isDirectory()) {
                    next.toFile().mkdirs();
                } else {
                    try (OutputStream out = Files.newOutputStream(next)) {
                        IOUtils.copy(tar, out);
                    }
                }
            }
        }
        Logger.debug(this, "Unpacked tar.gz %s to %s", this.file, dest);
    }

    /**
     * Unpacks gzip to the temp dir.
     * @param dest Destination directory
     * @throws IOException If fails
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public void unpack(final Path dest) throws IOException {
        try (OutputStream out = Files.newOutputStream(dest);
            GZIPInputStream input = new GZIPInputStream(Files.newInputStream(this.file))) {
            IOUtils.copy(input, out);
        }
        Logger.debug(this, "Unpacked gz %s to %s", this.file, dest);
    }
}
