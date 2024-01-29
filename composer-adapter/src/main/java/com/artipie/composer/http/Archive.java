/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ext.PublisherAs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionStage;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Interface for working with archive file. For example, obtaining
 * composer json file from archive.
 * @since 0.4
 */
public interface Archive {
    /**
     * Obtains composer json file from archive.
     * @param archive Content of archive file
     * @return Composer json file from archive.
     */
    CompletionStage<JsonObject> composerFrom(Content archive);

    /**
     * Replaces composer json file in existing archive with new one.
     * @param archive Archive with existing composer json
     * @param composer Composer json file that we will change the existing one to
     * @return Archive with replaced composer json file
     */
    CompletionStage<Content> replaceComposerWith(Content archive, byte[] composer);

    /**
     * Obtains archive name.
     * @return Archive name.
     */
    Archive.Name name();

    /**
     * Archive in ZIP format.
     * @since 0.4
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    class Zip implements Archive {
        /**
         * Composer json file name.
         */
        private static final String COMPOS = "composer.json";

        /**
         * Path to archive file with its name.
         */
        private final Name cname;

        /**
         * Ctor.
         * @param name Name of archive file
         */
        public Zip(final Name name) {
            this.cname = name;
        }

        @Override
        public CompletionStage<JsonObject> composerFrom(final Content archive) {
            return new PublisherAs(archive).bytes()
                .thenApply(
                    bytes -> {
                        try (
                            ZipArchiveInputStream zip = new ZipArchiveInputStream(
                                new ByteArrayInputStream(bytes)
                            )
                        ) {
                            ArchiveEntry entry;
                            while ((entry = zip.getNextZipEntry()) != null) {
                                final String[] parts = entry.getName().split("/");
                                if (Zip.COMPOS.equals(parts[parts.length - 1])) {
                                    return Json.createReader(zip).readObject();
                                }
                            }
                            throw new IllegalStateException(
                                String.format("'%s' file was not found", Zip.COMPOS)
                            );
                        } catch (final IOException exc) {
                            throw new UncheckedIOException(exc);
                        }
                    }
                );
        }

        @Override
        public Name name() {
            return this.cname;
        }

        // @checkstyle ExecutableStatementCountCheck (5 lines)
        @Override
        public CompletionStage<Content> replaceComposerWith(
            final Content archive, final byte[] composer
        ) {
            return new PublisherAs(archive)
                .bytes()
                .thenApply(
                    bytes -> {
                        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                            try (
                                ZipArchiveInputStream zip = new ZipArchiveInputStream(
                                    new ByteArrayInputStream(bytes)
                                )
                            ) {
                                ArchiveEntry entry;
                                while ((entry = zip.getNextZipEntry()) != null) {
                                    final ZipEntry newentr = new ZipEntry(entry.getName());
                                    final boolean isdir = newentr.isDirectory();
                                    final String[] parts = entry.getName().split("/");
                                    if (Zip.COMPOS.equals(parts[parts.length - 1]) && !isdir) {
                                        zos.putNextEntry(newentr);
                                        zos.write(composer);
                                    } else if (!isdir) {
                                        zos.putNextEntry(newentr);
                                        // @checkstyle MagicNumberCheck (1 line)
                                        final byte[] buf = new byte[1024];
                                        int len;
                                        while ((len = zip.read(buf)) > 0) {
                                            zos.write(buf, 0, len);
                                        }
                                    }
                                    zos.flush();
                                    zos.closeEntry();
                                }
                            }
                        } catch (final IOException exc) {
                            throw new UncheckedIOException(exc);
                        }
                        return bos.toByteArray();
                    }
                ).thenApply(Content.From::new);
        }
    }

    /**
     * Name of archive consisting of name and version.
     * For example, "name-1.0.1.tgz".
     * @since 0.4
     */
    class Name {
        /**
         * Full name.
         */
        private final String full;

        /**
         * Version which is extracted from the name.
         */
        private final String vrsn;

        /**
         * Ctor.
         * @param full Full name
         * @param vrsn Version from name
         */
        public Name(final String full, final String vrsn) {
            this.full = full;
            this.vrsn = vrsn;
        }

        /**
         * Obtains full name.
         * @return Full name.
         */
        public String full() {
            return this.full;
        }

        /**
         * Obtains artifact archive key as "artifacts" prefix and full name.
         * @return Full name.
         */
        public Key artifact() {
            return new Key.From("artifacts", this.full);
        }

        /**
         * Obtains version which is extracted from the name.
         * @return Version which is extracted from the name.
         */
        public String version() {
            return this.vrsn;
        }
    }
}
