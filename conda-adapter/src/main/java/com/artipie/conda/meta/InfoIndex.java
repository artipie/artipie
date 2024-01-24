/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.artipie.ArtipieException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

/**
 * Conda package metadata file info/index.json.
 * @since 0.2
 */
public interface InfoIndex {

    /**
     * Name of the package metadata file.
     */
    String FILE_NAME = "info/index.json";

    /**
     * Conda package metadata info/index.json content as json object.
     * @return Metadata json
     * @throws IOException On error
     */
    JsonObject json() throws IOException;

    /**
     * Implementation of {@link InfoIndex} to read metadata from `tar.bz2` conda package.
     * @since 0.2
     */
    final class TarBz implements InfoIndex {

        /**
         * Conda `tar.bz2` package as input stream.
         */
        private final InputStream input;

        /**
         * Ctor.
         * @param input Conda `tar.bz2` package as input stream
         */
        public TarBz(final InputStream input) {
            this.input = input;
        }

        @Override
        @SuppressWarnings("PMD.AssignmentInOperand")
        public JsonObject json() throws IOException {
            Optional<JsonObject> res = Optional.empty();
            try (
                TarArchiveInputStream archive = new TarArchiveInputStream(
                    new BZip2CompressorInputStream(this.input)
                )
            ) {
                ArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    if (!archive.canReadEntryData(entry) || entry.isDirectory()) {
                        continue;
                    }
                    if (InfoIndex.FILE_NAME.equals(entry.getName())) {
                        res = Optional.of(Json.createReader(archive).readObject());
                    }
                }
            }
            return res.orElseThrow(
                () -> new ArtipieException(
                    "Illegal package .tar.bz2: info/index.json file not found"
                )
            );
        }
    }

    /**
     * Implementation of {@link InfoIndex} to read metadata from `.conda` package.
     * @since 0.2
     */
    @SuppressWarnings("PMD.CognitiveComplexity")
    final class Conda implements InfoIndex {

        /**
         * Conda `.conda` package as input stream.
         */
        private final InputStream input;

        /**
         * Ctor.
         * @param input Conda `.conda` package as input stream
         */
        public Conda(final InputStream input) {
            this.input = input;
        }

        @Override
        @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.AssignmentInOperand", "PMD.CloseResource"})
        public JsonObject json() throws IOException {
            Optional<JsonObject> res = Optional.empty();
            try (
                ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(
                    new BufferedInputStream(this.input)
                )
            ) {
                ArchiveEntry entry;
                while ((entry = archive.getNextEntry()) != null) {
                    if (!archive.canReadEntryData(entry) || entry.isDirectory()) {
                        continue;
                    }
                    final String name = entry.getName();
                    if (name.startsWith("info") && name.endsWith("tar.zst")) {
                        final TarArchiveInputStream info = new TarArchiveInputStream(
                            new ZstdCompressorInputStream(archive)
                        );
                        while ((entry = info.getNextEntry()) != null) {
                            if (!info.canReadEntryData(entry) || entry.isDirectory()) {
                                continue;
                            }
                            if (InfoIndex.FILE_NAME.equals(entry.getName())) {
                                res = Optional.of(Json.createReader(info).readObject());
                            }
                        }
                    }
                }
            } catch (final ArchiveException ex) {
                throw new IOException(ex);
            }
            return res.orElseThrow(
                () -> new ArtipieException(
                    "Illegal package `.conda`: info/index.json file not found"
                )
            );
        }
    }
}
