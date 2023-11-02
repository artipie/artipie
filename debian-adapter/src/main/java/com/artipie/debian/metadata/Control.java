/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.metadata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Control metadata file from debian package.
 * See <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html">docs</a>.
 * @since 0.1
 */
public interface Control {

    /**
     * Control file content as string.
     * @return String with package info
     */
    String asString();

    /**
     * Control from debian binary package.
     * Check <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html#binary-package-control-files-debian-control">docs</a>.
     * @since 0.1
     */
    final class FromInputStream implements Control {

        /**
         * Control file name.
         */
        private static final String FILE_NAME = "control";

        /**
         * Debian binary package stream.
         */
        private final InputStream pkg;

        /**
         * Ctor.
         *
         * @param pkg Debian binary package
         */
        public FromInputStream(final InputStream pkg) {
            this.pkg = pkg;
        }

        @Override
        @SuppressWarnings("PMD.AssignmentInOperand")
        public String asString() {
            Optional<String> res = Optional.empty();
            try (
                ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(
                    new BufferedInputStream(this.pkg)
                )
            ) {
                ArchiveEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    if (!input.canReadEntryData(entry)) {
                        continue;
                    }
                    if (entry.getName().startsWith(FromInputStream.FILE_NAME)) {
                        res = Optional.of(
                            FromInputStream.unpackTar(
                                FromInputStream.stream(input, entry.getName())
                            )
                        );
                    }
                }
            } catch (final ArchiveException | IOException ex) {
                throw new IllegalStateException("Failed to obtain package metadata", ex);
            }
            return res.orElseThrow(
                () -> new IllegalStateException("Archive `control` is not found in the package")
            );
        }

        /**
         * Returns correct (depending on archive type) input stream for archive entry input stream.
         *
         * @param input Archived entry input
         * @param name Archived entry name
         * @return Corresponding InputStream instance
         * @throws IOException On error
         */
        private static InputStream stream(final ArchiveInputStream input, final String name)
            throws IOException {
            final InputStream res;
            if (name.endsWith("gz")) {
                res = new GzipCompressorInputStream(input);
            } else if (name.endsWith("xz")) {
                res = new XZCompressorInputStream(input);
            } else if (name.endsWith("zst")) {
                res = new ZstdCompressorInputStream(input);
            } else {
                throw new IllegalStateException("Unsupported archive type");
            }
            return res;
        }

        /**
         * Unpacks internal tar and reads control file.
         *
         * @param input Input stream
         * @return Control file as string
         * @throws IOException On error
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private static String unpackTar(final InputStream input) throws IOException {
            final TarArchiveInputStream tar = new TarArchiveInputStream(input);
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                if (entry.isFile()
                    && entry.getName().equals(String.format("./%s", FromInputStream.FILE_NAME))) {
                    return IOUtils.toString(tar, StandardCharsets.UTF_8);
                }
            }
            throw new IllegalStateException("File `control` is not found in `control` archive");
        }
    }
}
